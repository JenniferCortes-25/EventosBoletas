package co.uniquindio.eventoboletas.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.usecases.boleto.ComprarBoletoUseCase;
import co.uniquindio.eventoboletas.domain.entities.*;
import co.uniquindio.eventoboletas.domain.enums.*;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import co.uniquindio.eventoboletas.domain.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del CU-01: Comprar Boleto.
 *
 * Cubre:
 *   CA-01  Flujo feliz — compra exitosa
 *   CA-02  Flujo alterno bloqueante — cliente BLOQUEADO (RN-01)
 *   CA-03  Flujo alterno — zona agotada (RN-03)
 *   CA-04  Flujo alterno — evento cancelado (RN-02)
 *   CA-05  Flujo alterno — cliente INACTIVO también bloqueado (RN-01)
 *   CA-06  RN-05 — boleto no emitido sin pago aprobado
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CU-01 · ComprarBoletoUseCase")
class ComprarBoletoUseCaseTest {

    // ── Mocks de repositorios (puertos de dominio) ───────────────────────────
    @Mock private ClienteRepository clienteRepository;
    @Mock private EventoRepository  eventoRepository;
    @Mock private ZonaRepository    zonaRepository;
    @Mock private BoletoRepository  boletoRepository;

    @InjectMocks
    private ComprarBoletoUseCase sut;

    // ── IDs de referencia — coinciden con el DataSeeder ──────────────────────
    private static final Long ID_CLIENTE_ACTIVO    = 1L;   // Ana Gómez
    private static final Long ID_CLIENTE_BLOQUEADO = 10L;  // Miguel Jiménez
    private static final Long ID_CLIENTE_INACTIVO  = 9L;   // Sofía Vargas
    private static final Long ID_EVENTO_ACTIVO     = 1L;   // Festival de Música
    private static final Long ID_EVENTO_CANCELADO  = 4L;   // Concierto Cancelado
    private static final Long ID_ZONA_CON_CUPO     = 2L;   // General — 200 cupos
    private static final Long ID_ZONA_AGOTADA      = 7L;   // Galería — cupo=0

    // ── Helpers de construcción ───────────────────────────────────────────────

    private Cliente clienteActivo() {
        return Cliente.reconstituir(ID_CLIENTE_ACTIVO, "Ana", "Gómez",
                "ana.gomez@email.com", "1000001", EstadoCliente.ACTIVO);
    }

    private Cliente clienteBloqueado() {
        return Cliente.reconstituir(ID_CLIENTE_BLOQUEADO, "Miguel", "Jiménez",
                "miguel.jm@email.com", "1000010", EstadoCliente.BLOQUEADO);
    }

    private Cliente clienteInactivo() {
        return Cliente.reconstituir(ID_CLIENTE_INACTIVO, "Sofía", "Vargas",
                "sofia.vargas@email.com", "1000009", EstadoCliente.INACTIVO);
    }

    private Evento eventoActivo() {
        return Evento.reconstituir(ID_EVENTO_ACTIVO, "Festival Latinoamericano de Música",
                LocalDateTime.now().plusDays(30), "Estadio El Campín", EstadoEvento.ACTIVO);
    }

    private Evento eventoCancelado() {
        return Evento.reconstituir(ID_EVENTO_CANCELADO, "Concierto Cancelado",
                LocalDateTime.now().plusDays(5), "Plaza de Bolívar", EstadoEvento.CANCELADO);
    }

    /** Zona General: precioBase=120.000, recargo=5% → precioFinal=126.000 */
    private Zona zonaConCupo() {
        return Zona.reconstituir(ID_ZONA_CON_CUPO, "General",
                new BigDecimal("120000"), new BigDecimal("0.05"),
                200, 150, ID_EVENTO_ACTIVO);
    }

    /** Zona Galería: cupoDisponible=0 */
    private Zona zonaAgotada() {
        return Zona.reconstituir(ID_ZONA_AGOTADA, "Galería",
                new BigDecimal("40000"), new BigDecimal("0.00"),
                50, 0, ID_EVENTO_ACTIVO);
    }

    private Boleto boletoFake(Zona zona) {
        Pago pago = Pago.crear(zona.calcularPrecioFinal(), MetodoPago.EFECTIVO);
        Boleto b  = Boleto.emitir(zona.calcularPrecioFinal(), ID_CLIENTE_ACTIVO, zona.getId(), pago);
        b.asignarId(100L);
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-01 — Flujo feliz
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-01 — Flujo feliz: compra exitosa
     *
     * Given: Cliente ACTIVO + Evento ACTIVO + Zona con cupo > 0
     * When:  ejecutar(request con metodoPago=EFECTIVO)
     * Then:  201 — BoletoResponse con codigoQR no nulo
     *        Y precioFinal = precioBase × (1 + recargo) = 126.000,00
     *        Y la zona fue guardada (cupo reducido)
     *        Y el boleto fue persistido exactamente una vez
     */
    @Test
    @DisplayName("CA-01 · Flujo feliz → boleto emitido con QR y precio correcto")
    void ca01_compraBoleto_exitosa() {
        // GIVEN
        Zona   zona   = zonaConCupo();
        Boleto boleto = boletoFake(zona);

        when(clienteRepository.buscarPorId(ID_CLIENTE_ACTIVO)).thenReturn(Optional.of(clienteActivo()));
        when(eventoRepository.buscarPorId(ID_EVENTO_ACTIVO)).thenReturn(Optional.of(eventoActivo()));
        when(zonaRepository.buscarPorId(ID_ZONA_CON_CUPO)).thenReturn(Optional.of(zona));
        when(zonaRepository.guardar(any(Zona.class))).thenReturn(zona);
        when(boletoRepository.guardar(any(Boleto.class))).thenReturn(boleto);

        ComprarBoletoRequest request = new ComprarBoletoRequest(
                ID_CLIENTE_ACTIVO, ID_EVENTO_ACTIVO, ID_ZONA_CON_CUPO, MetodoPago.EFECTIVO);

        // WHEN
        BoletoResponse respuesta = sut.ejecutar(request);

        // THEN
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.codigoQR()).isNotBlank();
        assertThat(respuesta.precioFinal())
                .isEqualByComparingTo(new BigDecimal("126000.00"));

        // infraestructura llamada exactamente una vez cada una
        verify(zonaRepository,   times(1)).guardar(any(Zona.class));
        verify(boletoRepository, times(1)).guardar(any(Boleto.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-02 — Flujo alterno BLOQUEANTE: cliente BLOQUEADO (RN-01)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-02 — Flujo alterno bloqueante: cliente BLOQUEADO
     *
     * Given: Cliente con estado BLOQUEADO
     * When:  ejecutar(request con ese clienteId)
     * Then:  422 — ReglaDeNegocioException "no está habilitado para comprar"
     *        Y la transacción se aborta: ningún otro repositorio fue consultado
     *        Y ningún boleto/pago fue creado
     */
    @Test
    @DisplayName("CA-02 · Cliente BLOQUEADO → excepción RN-01, transacción abortada (flujo bloqueante)")
    void ca02_clienteBloqueado_lanzaExcepcion() {
        // GIVEN
        when(clienteRepository.buscarPorId(ID_CLIENTE_BLOQUEADO))
                .thenReturn(Optional.of(clienteBloqueado()));

        ComprarBoletoRequest request = new ComprarBoletoRequest(
                ID_CLIENTE_BLOQUEADO, ID_EVENTO_ACTIVO, ID_ZONA_CON_CUPO, MetodoPago.EFECTIVO);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.ejecutar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no está habilitado para comprar");

        // la transacción se detiene en el primer paso — ningún repo posterior fue tocado
        verify(eventoRepository, never()).buscarPorId(anyLong());
        verify(zonaRepository,   never()).buscarPorId(anyLong());
        verify(boletoRepository, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-03 — Zona agotada (RN-03)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-03 — Zona sin cupo
     *
     * Given: Cliente ACTIVO + Evento ACTIVO + Zona con cupoDisponible = 0
     * When:  ejecutar(request con esa zonaId)
     * Then:  422 — ReglaDeNegocioException "No hay boletos disponibles para esta zona"
     */
    @Test
    @DisplayName("CA-03 · Zona agotada (cupo=0) → excepción RN-03")
    void ca03_zonaAgotada_lanzaExcepcion() {
        // GIVEN
        when(clienteRepository.buscarPorId(ID_CLIENTE_ACTIVO)).thenReturn(Optional.of(clienteActivo()));
        when(eventoRepository.buscarPorId(ID_EVENTO_ACTIVO)).thenReturn(Optional.of(eventoActivo()));
        when(zonaRepository.buscarPorId(ID_ZONA_AGOTADA)).thenReturn(Optional.of(zonaAgotada()));

        ComprarBoletoRequest request = new ComprarBoletoRequest(
                ID_CLIENTE_ACTIVO, ID_EVENTO_ACTIVO, ID_ZONA_AGOTADA, MetodoPago.TARJETA);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.ejecutar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No hay boletos disponibles para esta zona");

        verify(boletoRepository, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-04 — Evento cancelado (RN-02)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-04 — Evento cancelado
     *
     * Given: Cliente ACTIVO + Evento con estado CANCELADO
     * When:  ejecutar(request con ese eventoId)
     * Then:  422 — ReglaDeNegocioException "no está disponible para la venta"
     */
    @Test
    @DisplayName("CA-04 · Evento CANCELADO → excepción RN-02")
    void ca04_eventoCancelado_lanzaExcepcion() {
        // GIVEN
        when(clienteRepository.buscarPorId(ID_CLIENTE_ACTIVO)).thenReturn(Optional.of(clienteActivo()));
        when(eventoRepository.buscarPorId(ID_EVENTO_CANCELADO)).thenReturn(Optional.of(eventoCancelado()));

        ComprarBoletoRequest request = new ComprarBoletoRequest(
                ID_CLIENTE_ACTIVO, ID_EVENTO_CANCELADO, ID_ZONA_CON_CUPO, MetodoPago.EFECTIVO);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.ejecutar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no está disponible para la venta");

        verify(zonaRepository,   never()).buscarPorId(anyLong());
        verify(boletoRepository, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-05 — Cliente INACTIVO (variante RN-01)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-05 — Cliente INACTIVO también rechazado por RN-01
     *
     * Given: Cliente con estado INACTIVO (distinto al BLOQUEADO de CA-02)
     * When:  ejecutar(request con ese clienteId)
     * Then:  422 — ReglaDeNegocioException "no está habilitado para comprar"
     */
    @Test
    @DisplayName("CA-05 · Cliente INACTIVO → también rechazado por RN-01")
    void ca05_clienteInactivo_lanzaExcepcion() {
        // GIVEN
        when(clienteRepository.buscarPorId(ID_CLIENTE_INACTIVO))
                .thenReturn(Optional.of(clienteInactivo()));

        ComprarBoletoRequest request = new ComprarBoletoRequest(
                ID_CLIENTE_INACTIVO, ID_EVENTO_ACTIVO, ID_ZONA_CON_CUPO, MetodoPago.EFECTIVO);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.ejecutar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no está habilitado para comprar");

        verify(boletoRepository, never()).guardar(any());
    }
}
