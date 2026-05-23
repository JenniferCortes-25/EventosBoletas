package co.uniquindio.eventoboletas.domain;

import co.uniquindio.eventoboletas.domain.entities.*;
import co.uniquindio.eventoboletas.domain.enums.*;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de reglas de negocio puras del dominio (sin Spring, sin Mockito).
 * Cada @Nested corresponde a una RN del enunciado.
 */
@DisplayName("Reglas de Negocio — Dominio Puro")
class DomainRulesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // RN-01 — Cliente habilitado para comprar
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-01 · El cliente debe tener estado ACTIVO para comprar")
    class Rn01ClienteHabilitado {

        @Test
        @DisplayName("Cliente ACTIVO → no lanza excepción")
        void clienteActivo_noLanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(1L, "Ana", "Gómez",
                    "ana@test.com", "123", EstadoCliente.ACTIVO);
            assertThatCode(cliente::verificarHabilitadoParaComprar)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Cliente BLOQUEADO → lanza ReglaDeNegocioException con mensaje correcto")
        void clienteBloqueado_lanzaExcepcionConMensaje() {
            Cliente cliente = Cliente.reconstituir(10L, "Miguel", "Jiménez",
                    "miguel@test.com", "456", EstadoCliente.BLOQUEADO);
            assertThatThrownBy(cliente::verificarHabilitadoParaComprar)
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está habilitado para comprar");
        }

        @Test
        @DisplayName("Cliente INACTIVO → lanza ReglaDeNegocioException con mensaje correcto")
        void clienteInactivo_lanzaExcepcionConMensaje() {
            Cliente cliente = Cliente.reconstituir(9L, "Sofía", "Vargas",
                    "sofia@test.com", "789", EstadoCliente.INACTIVO);
            assertThatThrownBy(cliente::verificarHabilitadoParaComprar)
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está habilitado para comprar");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-02 — Evento disponible para venta
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-02 · El evento debe estar ACTIVO para vender boletos")
    class Rn02EventoDisponible {

        @Test
        @DisplayName("Evento ACTIVO → no lanza excepción")
        void eventoActivo_noLanzaExcepcion() {
            Evento evento = Evento.reconstituir(1L, "Festival",
                    LocalDateTime.now().plusDays(10), "Bogotá", EstadoEvento.ACTIVO);
            assertThatCode(evento::verificarDisponibleParaVenta)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Evento CANCELADO → lanza ReglaDeNegocioException")
        void eventoCancelado_lanzaExcepcion() {
            Evento evento = Evento.reconstituir(4L, "Concierto Cancelado",
                    LocalDateTime.now().plusDays(5), "Armenia", EstadoEvento.CANCELADO);
            assertThatThrownBy(evento::verificarDisponibleParaVenta)
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está disponible para la venta");
        }

        @Test
        @DisplayName("Evento AGOTADO → lanza ReglaDeNegocioException")
        void eventoAgotado_lanzaExcepcion() {
            Evento evento = Evento.reconstituir(5L, "Evento Agotado",
                    LocalDateTime.now().plusDays(3), "Cali", EstadoEvento.AGOTADO);
            assertThatThrownBy(evento::verificarDisponibleParaVenta)
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-03 — Cupo disponible en la zona
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-03 · El cupo disponible debe ser >= cantidad solicitada")
    class Rn03CupoDisponible {

        @Test
        @DisplayName("Zona con cupo suficiente → no lanza excepción")
        void zonaConCupo_noLanzaExcepcion() {
            Zona zona = Zona.reconstituir(1L, "General",
                    new BigDecimal("120000"), new BigDecimal("0.05"),
                    200, 50, 1L);
            assertThatCode(() -> zona.verificarCupoDisponible(3))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Zona con cupo = 0 → lanza ReglaDeNegocioException")
        void zonaAgotada_lanzaExcepcion() {
            Zona zona = Zona.reconstituir(7L, "Galería",
                    new BigDecimal("40000"), new BigDecimal("0.00"),
                    50, 0, 1L);
            assertThatThrownBy(() -> zona.verificarCupoDisponible(1))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No hay suficientes boletos disponibles");
        }

        @Test
        @DisplayName("Solicitar más boletos que el cupo disponible → lanza excepción")
        void cupoInsuficiente_lanzaExcepcion() {
            Zona zona = Zona.reconstituir(2L, "VIP",
                    new BigDecimal("350000"), new BigDecimal("0.10"),
                    50, 2, 1L);
            assertThatThrownBy(() -> zona.verificarCupoDisponible(5))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No hay suficientes boletos disponibles");
        }

        @Test
        @DisplayName("reducirCupo(n) decrementa cupoDisponible correctamente")
        void reducirCupo_decrementaCantidadCorrecta() {
            Zona zona = Zona.reconstituir(1L, "VIP",
                    new BigDecimal("350000"), new BigDecimal("0.10"),
                    50, 10, 1L);
            zona.reducirCupo(3);
            assertThat(zona.getCupoDisponible()).isEqualTo(7);
        }

        @Test
        @DisplayName("reducirCupo en zona agotada → lanza excepción (invariante)")
        void reducirCupo_zonaAgotada_lanzaExcepcion() {
            Zona zona = Zona.reconstituir(7L, "Galería",
                    new BigDecimal("40000"), new BigDecimal("0.00"),
                    50, 0, 1L);
            assertThatThrownBy(() -> zona.reducirCupo(1))
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-04 — Precio calculado en el servidor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-04 · precioFinal = precioBase × (1 + recargo_zona + recargo_metodoPago)")
    class Rn04PrecioCalculado {

        @Test
        @DisplayName("EFECTIVO (0%): 120.000 × 1.05 = 126.000,00")
        void efectivo_sinRecargoMetodo() {
            Zona zona = zonaGeneral();
            assertThat(zona.calcularPrecioFinal(MetodoPago.EFECTIVO))
                    .isEqualByComparingTo(new BigDecimal("126000.00"));
        }

        @Test
        @DisplayName("PSE (1%): 120.000 × (1 + 0.05 + 0.01) = 127.200,00")
        void pse_recargo1pct() {
            Zona zona = zonaGeneral();
            assertThat(zona.calcularPrecioFinal(MetodoPago.PSE))
                    .isEqualByComparingTo(new BigDecimal("127200.00"));
        }

        @Test
        @DisplayName("TARJETA_DEBITO (2%): 120.000 × (1 + 0.05 + 0.02) = 128.400,00")
        void debito_recargo2pct() {
            Zona zona = zonaGeneral();
            assertThat(zona.calcularPrecioFinal(MetodoPago.TARJETA_DEBITO))
                    .isEqualByComparingTo(new BigDecimal("128400.00"));
        }

        @Test
        @DisplayName("TRANSFERENCIA (1.5%): 120.000 × (1 + 0.05 + 0.015) = 127.800,00")
        void transferencia_recargo1_5pct() {
            Zona zona = zonaGeneral();
            assertThat(zona.calcularPrecioFinal(MetodoPago.TRANSFERENCIA))
                    .isEqualByComparingTo(new BigDecimal("127800.00"));
        }

        @Test
        @DisplayName("TARJETA_CREDITO (5%): 120.000 × (1 + 0.05 + 0.05) = 132.000,00")
        void credito_recargo5pct() {
            Zona zona = zonaGeneral();
            assertThat(zona.calcularPrecioFinal(MetodoPago.TARJETA_CREDITO))
                    .isEqualByComparingTo(new BigDecimal("132000.00"));
        }

        @Test
        @DisplayName("Zona sin recargo propio + EFECTIVO: precio final = precio base")
        void sinRecargoZona_efectivo_igualAlBase() {
            Zona zona = Zona.reconstituir(5L, "Libre",
                    new BigDecimal("30000"), BigDecimal.ZERO, 300, 300, 2L);
            assertThat(zona.calcularPrecioFinal(MetodoPago.EFECTIVO))
                    .isEqualByComparingTo(new BigDecimal("30000.00"));
        }

        // Helper
        private Zona zonaGeneral() {
            return Zona.reconstituir(2L, "General",
                    new BigDecimal("120000"), new BigDecimal("0.05"),
                    200, 200, 1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-05 — Boleto solo con pago APROBADO
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-05 · El boleto solo se emite con pago APROBADO")
    class Rn05BoletoConPagoAprobado {

        @Test
        @DisplayName("Pago APROBADO → boleto emitido con QR único y estado PAGADO")
        void pagoAprobado_emiteBoletoConQr() {
            Pago pago = Pago.crear(new BigDecimal("126000"), MetodoPago.EFECTIVO, EstadoPago.APROBADO);
            Boleto boleto = Boleto.emitir(new BigDecimal("126000"), 1L, 2L, pago);

            assertThat(boleto.getCodigoQR()).isNotBlank();
            assertThat(boleto.getEstado()).isEqualTo(EstadoBoleto.PAGADO);
            assertThat(boleto.getPago().getEstado()).isEqualTo(EstadoPago.APROBADO);
        }

        @Test
        @DisplayName("Pago RECHAZADO → lanza ReglaDeNegocioException, no se emite boleto")
        void pagoRechazado_noEmiteBoleto() {
            Pago pago = Pago.reconstituir(null, new BigDecimal("126000"),
                    MetodoPago.TARJETA_DEBITO, LocalDateTime.now(), EstadoPago.RECHAZADO);
            assertThatThrownBy(() -> Boleto.emitir(new BigDecimal("126000"), 1L, 2L, pago))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("pago aprobado");
        }


        @Test
        @DisplayName("Dos boletos emitidos tienen códigos QR distintos (unicidad)")
        void dosboletos_tienenQrUnicos() {
            Pago p1 = Pago.crear(new BigDecimal("126000"), MetodoPago.EFECTIVO, EstadoPago.APROBADO);
            Pago p2 = Pago.crear(new BigDecimal("126000"), MetodoPago.EFECTIVO, EstadoPago.APROBADO);
            Boleto b1 = Boleto.emitir(new BigDecimal("126000"), 1L, 2L, p1);
            Boleto b2 = Boleto.emitir(new BigDecimal("126000"), 1L, 2L, p2);
            assertThat(b1.getCodigoQR()).isNotEqualTo(b2.getCodigoQR());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-06 — Cliente eliminable
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-06 · No se puede eliminar un cliente con boletos PAGADOS")
    class Rn06ClienteEliminable {

        @Test
        @DisplayName("Cliente sin boletos → verificarEliminable no lanza excepción")
        void sinBoletos_noLanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(12L, "Sergio", "Medina",
                    "sergio@test.com", "1000012", EstadoCliente.ACTIVO);
            assertThatCode(() -> cliente.verificarEliminable(false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Cliente con boletos PAGADOS → lanza excepción con mensaje correcto")
        void conBoletosPagados_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(1L, "Ana", "Gómez",
                    "ana@test.com", "1000001", EstadoCliente.ACTIVO);
            assertThatThrownBy(() -> cliente.verificarEliminable(true))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No se puede eliminar un cliente con boletos activos");
        }
    }
}