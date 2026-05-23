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
 * Prueban directamente los métodos de las entidades de dominio.
 *
 * Cubre:
 *   RN-01  Cliente: verificarHabilitadoParaComprar
 *   RN-02  Evento:  verificarDisponibleParaVenta
 *   RN-03  Zona:    verificarCupoDisponible y reducirCupo
 *   RN-04  Zona:    calcularPrecioFinal (fórmula servidor)
 *   RN-05  Boleto:  emitir sólo con pago APROBADO
 *   RN-06  Cliente: verificarEliminable
 */
@DisplayName("Reglas de Negocio — Dominio Puro")
class DomainRulesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // RN-01 — Cliente habilitado para comprar
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-01 · Cliente habilitado para comprar")
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
        @DisplayName("Cliente BLOQUEADO → lanza ReglaDeNegocioException")
        void clienteBloqueado_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(10L, "Miguel", "Jiménez",
                    "miguel@test.com", "456", EstadoCliente.BLOQUEADO);
            assertThatThrownBy(cliente::verificarHabilitadoParaComprar)
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("no está habilitado para comprar");
        }

        @Test
        @DisplayName("Cliente INACTIVO → lanza ReglaDeNegocioException")
        void clienteInactivo_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(9L, "Sofía", "Vargas",
                    "sofia@test.com", "789", EstadoCliente.INACTIVO);
            assertThatThrownBy(cliente::verificarHabilitadoParaComprar)
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-02 — Evento disponible para venta
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-02 · Evento disponible para venta")
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
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-03 — Zona: cupo disponible
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-03 · Zona: cupo disponible")
    class Rn03CupoDisponible {

        @Test
        @DisplayName("Zona con cupo > 0 → no lanza excepción")
        void zonaConCupo_noLanzaExcepcion() {
            Zona zona = Zona.reconstituir(1L, "General",
                    new BigDecimal("120000"), new BigDecimal("0.05"),
                    200, 50, 1L);
            assertThatCode(zona::verificarCupoDisponible)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Zona con cupo = 0 → lanza ReglaDeNegocioException")
        void zonaAgotada_lanzaExcepcion() {
            Zona zona = Zona.reconstituir(7L, "Galería",
                    new BigDecimal("40000"), new BigDecimal("0.00"),
                    50, 0, 1L);
            assertThatThrownBy(zona::verificarCupoDisponible)
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No hay boletos disponibles para esta zona");
        }

        @Test
        @DisplayName("reducirCupo() decrementa cupoDisponible en 1")
        void reducirCupo_decrementaEnUno() {
            Zona zona = Zona.reconstituir(1L, "VIP",
                    new BigDecimal("350000"), new BigDecimal("0.10"),
                    50, 10, 1L);
            zona.reducirCupo();
            assertThat(zona.getCupoDisponible()).isEqualTo(9);
        }

        @Test
        @DisplayName("reducirCupo() en zona agotada → lanza excepción (invariante)")
        void reducirCupo_zonaAgotada_lanzaExcepcion() {
            Zona zona = Zona.reconstituir(7L, "Galería",
                    new BigDecimal("40000"), new BigDecimal("0.00"),
                    50, 0, 1L);
            assertThatThrownBy(zona::reducirCupo)
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-04 — Precio calculado en dominio (servidor)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-04 · Precio calculado en el dominio")
    class Rn04PrecioCalculado {

        @Test
        @DisplayName("VIP: 350.000 × 1,10 = 385.000,00")
        void precioVip_calculoCorrecto() {
            Zona zonaVip = Zona.reconstituir(1L, "VIP",
                    new BigDecimal("350000"), new BigDecimal("0.10"),
                    50, 50, 1L);
            assertThat(zonaVip.calcularPrecioFinal())
                    .isEqualByComparingTo(new BigDecimal("385000.00"));
        }

        @Test
        @DisplayName("General: 120.000 × 1,05 = 126.000,00")
        void precioGeneral_calculoCorrecto() {
            Zona zonaGeneral = Zona.reconstituir(2L, "General",
                    new BigDecimal("120000"), new BigDecimal("0.05"),
                    200, 200, 1L);
            assertThat(zonaGeneral.calcularPrecioFinal())
                    .isEqualByComparingTo(new BigDecimal("126000.00"));
        }

        @Test
        @DisplayName("Libre sin recargo: 30.000 × 1,00 = 30.000,00")
        void precioSinRecargo_igualAlBase() {
            Zona zonaLibre = Zona.reconstituir(5L, "Libre",
                    new BigDecimal("30000"), BigDecimal.ZERO,
                    300, 300, 2L);
            assertThat(zonaLibre.calcularPrecioFinal())
                    .isEqualByComparingTo(new BigDecimal("30000.00"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-05 — Boleto sólo con pago aprobado
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-05 · Boleto emitido sólo con pago APROBADO")
    class Rn05BoletoConPagoAprobado {

        @Test
        @DisplayName("Pago APROBADO → boleto emitido con QR único")
        void pagoAprobado_emiteBoleto() {
            Pago pago = Pago.crear(new BigDecimal("126000"), MetodoPago.EFECTIVO, EstadoPago.APROBADO);
            Boleto boleto = Boleto.emitir(new BigDecimal("126000"), 1L, 2L, pago);

            assertThat(boleto.getCodigoQR()).isNotBlank();
            assertThat(boleto.getEstado()).isEqualTo(EstadoBoleto.PAGADO);
            assertThat(boleto.getPago().getEstado()).isEqualTo(EstadoPago.APROBADO);
        }

        @Test
        @DisplayName("Pago NO aprobado → lanza ReglaDeNegocioException (RN-05)")
        void pagoRechazado_noEmiteBoleto() {
            // Construimos un pago con estado no aprobado usando reconstituir
            Pago pagoRechazado = Pago.reconstituir(null, new BigDecimal("126000"),
                    MetodoPago.EFECTIVO, LocalDateTime.now(), EstadoPago.RECHAZADO);

            assertThatThrownBy(() ->
                    Boleto.emitir(new BigDecimal("126000"), 1L, 2L, pagoRechazado))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("pago aprobado");
        }

        @Test
        @DisplayName("Dos boletos emitidos tienen códigos QR distintos")
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
    @DisplayName("RN-06 · Cliente no eliminable con boletos activos")
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
        @DisplayName("Cliente con boletos PAGADOS → verificarEliminable lanza excepción")
        void conBoletos_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(1L, "Ana", "Gómez",
                    "ana@test.com", "1000001", EstadoCliente.ACTIVO);
            assertThatThrownBy(() -> cliente.verificarEliminable(true))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No se puede eliminar un cliente con boletos activos");
        }
    }
}