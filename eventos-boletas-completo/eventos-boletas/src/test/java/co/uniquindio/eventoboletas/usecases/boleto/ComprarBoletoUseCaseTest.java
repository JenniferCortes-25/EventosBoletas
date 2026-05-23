package co.uniquindio.eventoboletas.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.usecases.boleto.ComprarBoletoUseCase;
import co.uniquindio.eventoboletas.domain.entities.*;
import co.uniquindio.eventoboletas.domain.enums.*;
import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import co.uniquindio.eventoboletas.domain.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CU-01 · ComprarBoletoUseCase — Tests unitarios con Mockito")
class ComprarBoletoUseCaseTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private EventoRepository  eventoRepository;
    @Mock private ZonaRepository    zonaRepository;
    @Mock private BoletoRepository  boletoRepository;

    @InjectMocks
    private ComprarBoletoUseCase sut;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cliente clienteActivo() {
        return Cliente.reconstituir(1L, "Ana", "Gómez",
                "ana@test.com", "1000001", EstadoCliente.ACTIVO);
    }

    private Cliente clienteBloqueado() {
        return Cliente.reconstituir(10L, "Miguel", "Jiménez",
                "miguel@test.com", "1000010", EstadoCliente.BLOQUEADO);
    }

    private Cliente clienteInactivo() {
        return Cliente.reconstituir(9L, "Sofía", "Vargas",
                "sofia@test.com", "1000009", EstadoCliente.INACTIVO);
    }

    private Evento eventoActivo() {
        return Evento.reconstituir(1L, "Festival",
                LocalDateTime.now().plusDays(30), "Bogotá", EstadoEvento.ACTIVO);
    }

    private Evento eventoCancelado() {
        return Evento.reconstituir(4L, "Concierto Cancelado",
                LocalDateTime.now().plusDays(5), "Armenia", EstadoEvento.CANCELADO);
    }

    private Zona zonaConCupo() {
        // precioBase=120.000, recargo zona=5%
        return Zona.reconstituir(2L, "General",
                new BigDecimal("120000"), new BigDecimal("0.05"),
                200, 150, 1L);
    }

    private Zona zonaAgotada() {
        return Zona.reconstituir(7L, "Galería",
                new BigDecimal("40000"), BigDecimal.ZERO,
                50, 0, 1L);
    }

    private Boleto boletoFake(BigDecimal precio) {
        Pago pago = Pago.crear(precio, MetodoPago.EFECTIVO, EstadoPago.APROBADO);
        Boleto b  = Boleto.emitir(precio, 1L, 2L, pago);
        b.asignarId(100L);
        return b;
    }

    private void stubFlujoFeliz(Zona zona) {
        when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(clienteActivo()));
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(eventoActivo()));
        when(zonaRepository.buscarPorId(zona.getId())).thenReturn(Optional.of(zona));
        when(zonaRepository.guardar(any())).thenReturn(zona);
        when(boletoRepository.guardar(any()))
                .thenAnswer(inv -> { Boleto b = inv.getArgument(0); b.asignarId(100L); return b; });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-01 — Cliente habilitado
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-01 · Cliente debe estar ACTIVO")
    class Rn01 {

        @Test
        @DisplayName("Cliente ACTIVO + datos válidos → retorna lista con un BoletoResponse")
        void clienteActivo_retornaBoleto() {
            Zona zona = zonaConCupo();
            stubFlujoFeliz(zona);

            List<BoletoResponse> resultado = sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 2L, MetodoPago.EFECTIVO, 1));

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).codigoQR()).isNotBlank();
        }

        @Test
        @DisplayName("Cliente BLOQUEADO → ReglaDeNegocioException, repositorios posteriores no tocados")
        void clienteBloqueado_abortaTransaccion() {
            when(clienteRepository.buscarPorId(10L)).thenReturn(Optional.of(clienteBloqueado()));

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(10L, 1L, 2L, MetodoPago.EFECTIVO, 1)))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está habilitado para comprar");

            verify(eventoRepository,  never()).buscarPorId(anyLong());
            verify(zonaRepository,    never()).buscarPorId(anyLong());
            verify(boletoRepository,  never()).guardar(any());
        }

        @Test
        @DisplayName("Cliente INACTIVO → también rechazado por RN-01")
        void clienteInactivo_abortaTransaccion() {
            when(clienteRepository.buscarPorId(9L)).thenReturn(Optional.of(clienteInactivo()));

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(9L, 1L, 2L, MetodoPago.EFECTIVO, 1)))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está habilitado para comprar");

            verify(boletoRepository, never()).guardar(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-02 — Evento activo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-02 · Evento debe estar ACTIVO")
    class Rn02 {

        @Test
        @DisplayName("Evento CANCELADO → ReglaDeNegocioException, zona y boleto no consultados")
        void eventoCancelado_abortaTransaccion() {
            when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(clienteActivo()));
            when(eventoRepository.buscarPorId(4L)).thenReturn(Optional.of(eventoCancelado()));

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(1L, 4L, 2L, MetodoPago.EFECTIVO, 1)))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está disponible para la venta");

            verify(zonaRepository,   never()).buscarPorId(anyLong());
            verify(boletoRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Evento inexistente → EntidadNoEncontradaException")
        void eventoNoExiste_lanzaEntidadNoEncontrada() {
            when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(clienteActivo()));
            when(eventoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(1L, 99L, 2L, MetodoPago.EFECTIVO, 1)))
                    .isInstanceOf(EntidadNoEncontradaException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-03 — Cupo disponible
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-03 · Cupo disponible >= cantidad solicitada")
    class Rn03 {

        @Test
        @DisplayName("Zona agotada (cupo=0) → ReglaDeNegocioException, no se persiste boleto")
        void zonaAgotada_abortaTransaccion() {
            when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(clienteActivo()));
            when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(eventoActivo()));
            when(zonaRepository.buscarPorId(7L)).thenReturn(Optional.of(zonaAgotada()));

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 7L, MetodoPago.EFECTIVO, 1)))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No hay suficientes boletos disponibles");

            verify(boletoRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Solicitar 3 boletos con cupo=2 → excepción por cupo insuficiente")
        void cupoInsuficienteParaCantidad_lanzaExcepcion() {
            Zona zonaPocoCupo = Zona.reconstituir(2L, "VIP",
                    new BigDecimal("350000"), new BigDecimal("0.10"), 50, 2, 1L);

            when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(clienteActivo()));
            when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(eventoActivo()));
            when(zonaRepository.buscarPorId(2L)).thenReturn(Optional.of(zonaPocoCupo));

            assertThatThrownBy(() -> sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 2L, MetodoPago.EFECTIVO, 3)))
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-04 — Precio calculado en el servidor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-04 · Precio final calculado en servidor con recargo de método de pago")
    class Rn04 {

        // Zona General: precioBase=120.000, recargo_zona=5%

        @Test
        @DisplayName("EFECTIVO (0%): precioFinal = 120.000 × 1.05 = 126.000")
        void efectivo_precioSinRecargoMetodo() {
            verificarPrecio(MetodoPago.EFECTIVO, "126000.00");
        }

        @Test
        @DisplayName("PSE (1%): precioFinal = 120.000 × 1.06 = 127.200")
        void pse_precioConRecargo1pct() {
            verificarPrecio(MetodoPago.PSE, "127200.00");
        }

        @Test
        @DisplayName("TARJETA_DEBITO (2%): precioFinal = 120.000 × 1.07 = 128.400")
        void debito_precioConRecargo2pct() {
            verificarPrecio(MetodoPago.TARJETA_DEBITO, "128400.00");
        }

        @Test
        @DisplayName("TRANSFERENCIA (1.5%): precioFinal = 120.000 × 1.065 = 127.800")
        void transferencia_precioConRecargo1_5pct() {
            verificarPrecio(MetodoPago.TRANSFERENCIA, "127800.00");
        }

        @Test
        @DisplayName("TARJETA_CREDITO (5%): precioFinal = 120.000 × 1.10 = 132.000")
        void credito_precioConRecargo5pct() {
            verificarPrecio(MetodoPago.TARJETA_CREDITO, "132000.00");
        }

        @Test
        @DisplayName("Compra de 2 boletos → precio unitario correcto en cada boleto")
        void dosBoletos_precioUnitarioCorrecto() {
            Zona zona = zonaConCupo();
            stubFlujoFeliz(zona);

            List<BoletoResponse> resultado = sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 2L, MetodoPago.EFECTIVO, 2));

            assertThat(resultado).hasSize(2);
            resultado.forEach(b ->
                assertThat(b.precioFinal()).isEqualByComparingTo(new BigDecimal("126000.00")));
        }

        private void verificarPrecio(MetodoPago metodo, String precioEsperado) {
            Zona zona = zonaConCupo();
            stubFlujoFeliz(zona);

            List<BoletoResponse> resultado = sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 2L, metodo, 1));

            assertThat(resultado.get(0).precioFinal())
                    .isEqualByComparingTo(new BigDecimal(precioEsperado));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-05 — Boleto solo con pago APROBADO
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-05 · Boleto emitido solo con pago APROBADO")
    class Rn05 {

        @Test
        @DisplayName("EFECTIVO → siempre aprobado, boleto siempre emitido")
        void efectivo_siempreAprobado() {
            Zona zona = zonaConCupo();
            stubFlujoFeliz(zona);

            List<BoletoResponse> resultado = sut.ejecutar(
                    new ComprarBoletoRequest(1L, 1L, 2L, MetodoPago.EFECTIVO, 1));

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).pago().estado()).isEqualTo(EstadoPago.APROBADO);
        }
    }
}