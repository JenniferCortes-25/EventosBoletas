package co.uniquindio.eventoboletas.integration;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;
import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración con Spring Boot + H2 + DataSeeder real.
 *
 * Levanta el contexto completo (controllers, use cases, JPA, H2)
 * y ejercita los endpoints HTTP de extremo a extremo.
 *
 * Endpoint real del controller: POST /api/transaccion/comprar-boleto
 * El flujo feliz retorna 200 OK (ResponseEntity.ok(...))
 * Los errores de regla de negocio retornan 422 (GlobalExceptionHandler)
 *
 * IDs del DataSeeder (orden de inserción):
 *   Clientes: Ana=1, Carlos=2, ..., Sofía=9(INACTIVO), Miguel=10(BLOQUEADO)
 *   Zonas:    VIP=1, General=2, Palco=3, Premium=4, Libre=5, Butaca=6, Galería=7(cupo=0)
 *   Eventos:  Festival=1, Conferencia=2, Teatro=3, Cancelado=4
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Integración HTTP · CU-01 Comprar Boleto")
class ComprarBoletoIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // Endpoint real del BoletoController
    private static final String URL_COMPRAR = "/api/transaccion/comprar-boleto";
    private static final String URL_BUSCAR_CLIENTES = "/api/transaccion/clientes/buscar";
    private static final String URL_EVENTOS_ACTIVOS = "/api/transaccion/eventos/listar-activos";

    // ─────────────────────────────────────────────────────────────────────────
    // IT-01 — Flujo feliz HTTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-01 — Flujo feliz: POST /api/transaccion/comprar-boleto
     *
     * Given: cliente id=1 (Ana Gómez, ACTIVO)
     *        evento id=1 (Festival Latinoamericano, ACTIVO)
     *        zona  id=2 (General, precioBase=120.000, recargo=5%, cupo=200)
     * When:  POST /api/transaccion/comprar-boleto { clienteId:1, eventoId:1, zonaId:2, metodoPago:EFECTIVO }
     * Then:  200 OK
     *        Body tiene codigoQR no vacío
     *        Body tiene estadoBoleto = PAGADO
     *        Body tiene precioFinal = 126.000,00  (120.000 × 1,05)
     */
    @Test
    @DisplayName("IT-01 · Flujo feliz → 200 con QR y precio correcto")
    void it01_compraBoleto_exitosa() throws Exception {
        // GIVEN
        ComprarBoletoRequest request = new ComprarBoletoRequest(1L, 1L, 2L, MetodoPago.EFECTIVO);

        // WHEN
        MvcResult resultado = mockMvc.perform(post(URL_COMPRAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        BoletoResponse respuesta = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), BoletoResponse.class);

        assertThat(respuesta.codigoQR()).isNotBlank();
        assertThat(respuesta.estadoBoleto()).isEqualTo(EstadoBoleto.PAGADO);
        assertThat(respuesta.precioFinal().stripTrailingZeros())
                .isEqualByComparingTo("126000");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IT-02 — Flujo alterno bloqueante: cliente BLOQUEADO (RN-01)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-02 — Flujo alterno bloqueante: cliente BLOQUEADO
     *
     * Given: cliente id=10 (Miguel Jiménez, estado=BLOQUEADO)
     *        evento y zona válidos
     * When:  POST /api/transaccion/comprar-boleto con clienteId=10
     * Then:  422 Unprocessable Entity  (GlobalExceptionHandler captura ReglaDeNegocioException)
     */
    @Test
    @DisplayName("IT-02 · Cliente BLOQUEADO → 422 HTTP (flujo alterno bloqueante)")
    void it02_clienteBloqueado_retorna422() throws Exception {
        // GIVEN
        ComprarBoletoRequest request = new ComprarBoletoRequest(10L, 1L, 2L, MetodoPago.EFECTIVO);

        // WHEN + THEN
        mockMvc.perform(post(URL_COMPRAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IT-03 — Evento cancelado (RN-02)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-03 — Evento CANCELADO
     *
     * Given: cliente id=1 (ACTIVO)
     *        evento id=4 (Concierto Cancelado, estado=CANCELADO)
     * When:  POST /api/transaccion/comprar-boleto con eventoId=4
     * Then:  422 Unprocessable Entity
     */
    @Test
    @DisplayName("IT-03 · Evento CANCELADO → 422 HTTP")
    void it03_eventoCancelado_retorna422() throws Exception {
        // GIVEN
        ComprarBoletoRequest request = new ComprarBoletoRequest(1L, 4L, 2L, MetodoPago.EFECTIVO);

        // WHEN + THEN
        mockMvc.perform(post(URL_COMPRAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IT-04 — Zona agotada (RN-03)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-04 — Zona agotada (Galería, cupo=0)
     *
     * Given: cliente id=1 (ACTIVO)
     *        evento id=3 (Teatro, ACTIVO)
     *        zona  id=7 (Galería, cupoDisponible=0)
     *        El DataSeeder inserta zonas en cascada en orden:
     *        VIP(1), General(2), Palco(3) → Festival
     *        Premium(4), Libre(5)         → Conferencia
     *        Butaca(6), Galería(7)        → Teatro  ← cupo=0
     * When:  POST /api/transaccion/comprar-boleto con zonaId=7
     * Then:  422 Unprocessable Entity
     */
    @Test
    @DisplayName("IT-04 · Zona agotada (Galería, cupo=0) → 422 HTTP")
    void it04_zonaAgotada_retorna422() throws Exception {
        // GIVEN
        ComprarBoletoRequest request = new ComprarBoletoRequest(1L, 3L, 7L, MetodoPago.EFECTIVO);

        // WHEN + THEN
        mockMvc.perform(post(URL_COMPRAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IT-05 — Buscar clientes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-05 — GET /api/transaccion/clientes/buscar
     *
     * Given: DataSeeder tiene clientes cargados (Ana Gómez entre ellos)
     * When:  GET /api/transaccion/clientes/buscar?q=ana
     * Then:  200 OK con array JSON no vacío
     */
    @Test
    @DisplayName("IT-05 · Buscar clientes por nombre → 200 con resultados")
    void it05_buscarClientes_retornaResultados() throws Exception {
        mockMvc.perform(get(URL_BUSCAR_CLIENTES).param("q", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IT-06 — Listar eventos activos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * IT-06 — GET /api/transaccion/eventos/listar-activos
     *
     * Given: DataSeeder carga 3 eventos ACTIVOS + 1 CANCELADO
     * When:  GET /api/transaccion/eventos/listar-activos
     * Then:  200 OK con exactamente 3 eventos (el CANCELADO no aparece)
     */
    @Test
    @DisplayName("IT-06 · Listar eventos activos → sólo los 3 ACTIVOS (el CANCELADO no aparece)")
    void it06_listarEventosActivos_soloActivos() throws Exception {
        mockMvc.perform(get(URL_EVENTOS_ACTIVOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
