package co.uniquindio.eventoboletas.integration;

import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Integración HTTP · CU-01 Comprar Boleto")
class ComprarBoletoIntegrationTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  mapper;

    private static final String URL = "/api/transaccion/comprar-boleto";

    private String body(long clienteId, long eventoId, long zonaId, String metodo, int cantidad) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "clienteId", clienteId, "eventoId", eventoId,
                "zonaId", zonaId, "metodoPago", metodo, "cantidad", cantidad));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-01
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-01 · Cliente debe estar ACTIVO (HTTP)")
    class Rn01Http {

        @Test
        @DisplayName("IT-01 · Cliente ACTIVO + EFECTIVO → 200 con QR y precio 126.000")
        void clienteActivo_retorna200() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 1, 2, "EFECTIVO", 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].codigoQR", not(emptyString())))
                    .andExpect(jsonPath("$[0].precioFinal", is(126000.0)));
        }

        @Test
        @DisplayName("IT-02 · Cliente BLOQUEADO → 422")
        void clienteBloqueado_retorna422() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(10, 1, 2, "EFECTIVO", 1)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("no está habilitado")));
        }

        @Test
        @DisplayName("IT-03 · Cliente INACTIVO → 422")
        void clienteInactivo_retorna422() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(9, 1, 2, "EFECTIVO", 1)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("no está habilitado")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-02
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-02 · Evento debe estar ACTIVO (HTTP)")
    class Rn02Http {

        @Test
        @DisplayName("IT-04 · Evento CANCELADO → 422")
        void eventoCancelado_retorna422() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 4, 2, "EFECTIVO", 1)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("no está disponible")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-03
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-03 · Cupo disponible >= cantidad (HTTP)")
    class Rn03Http {

        @Test
        @DisplayName("IT-05 · Zona Galería (cupo=0) → 422")
        void zonaAgotada_retorna422() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 3, 7, "EFECTIVO", 1)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("No hay suficientes boletos")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-04
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-04 · Precio calculado en servidor según método de pago (HTTP)")
    class Rn04Http {

        // Zona General (id=2): precioBase=120.000, recargo_zona=5%

        @Test
        @DisplayName("IT-06 · EFECTIVO → precio 126.000")
        void efectivo_precio126000() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 1, 2, "EFECTIVO", 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].precioFinal", is(126000.0)));
        }

        @Test
        @DisplayName("IT-07 · PSE → precio 127.200")
        void pse_precio127200() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 1, 2, "PSE", 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].precioFinal", is(127200.0)));
        }

        @Test
        @DisplayName("IT-08 · TARJETA_CREDITO → precio 132.000")
        void credito_precio132000() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 1, 2, "TARJETA_CREDITO", 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].precioFinal", is(132000.0)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-05
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-05 · QR generado solo con pago APROBADO (HTTP)")
    class Rn05Http {

        @Test
        @DisplayName("IT-09 · EFECTIVO siempre aprobado → boleto con QR y estado PAGADO")
        void efectivo_boletoConQr() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                            .content(body(1, 1, 2, "EFECTIVO", 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].codigoQR", not(emptyString())))
                    .andExpect(jsonPath("$[0].estadoBoleto", is("PAGADO")));
        }
    }
}