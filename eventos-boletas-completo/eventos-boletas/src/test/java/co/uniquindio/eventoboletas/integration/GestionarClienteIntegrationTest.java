package co.uniquindio.eventoboletas.integration;

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
@DisplayName("Integración HTTP · CU-02 Gestionar Cliente")
class GestionarClienteIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    private static final String URL_CREAR    = "/api/clientes/crear";
    private static final String URL_EDITAR   = "/api/clientes/{id}/editar";
    private static final String URL_ELIMINAR = "/api/clientes/{id}/eliminar";
    private static final String URL_COMPRAR  = "/api/transaccion/comprar-boleto";

    // ─────────────────────────────────────────────────────────────────────────
    // RN-06 — No eliminar cliente con boletos pagados
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-06 · No eliminar cliente con boletos PAGADOS (HTTP)")
    class Rn06Http {

        @Test
        @DisplayName("IT-10 · Eliminar cliente sin boletos → 200 OK")
        void eliminarSinBoletos_retorna200() throws Exception {
            // Sergio Medina (id=12) no tiene boletos en el seeder
            mockMvc.perform(delete(URL_ELIMINAR, 12))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("IT-11 · Eliminar cliente que acaba de comprar → 422")
        void eliminarConBoletos_retorna422() throws Exception {
            // Primero comprar un boleto con EFECTIVO (siempre aprobado)
            String bodyCompra = mapper.writeValueAsString(Map.of(
                    "clienteId", 1, "eventoId", 1, "zonaId", 2,
                    "metodoPago", "EFECTIVO", "cantidad", 1));
            mockMvc.perform(post(URL_COMPRAR)
                    .contentType(MediaType.APPLICATION_JSON).content(bodyCompra))
                    .andExpect(status().isOk());

            // Luego intentar eliminar ese cliente
            mockMvc.perform(delete(URL_ELIMINAR, 1))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message",
                            containsString("No se puede eliminar un cliente con boletos activos")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-07 — Email único
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-07 · Email único en el sistema (HTTP)")
    class Rn07Http {

        @Test
        @DisplayName("IT-12 · Crear cliente con email nuevo → 201 con ID asignado")
        void crearEmailNuevo_retorna201() throws Exception {
            String body = mapper.writeValueAsString(Map.of(
                    "nombre", "Nuevo", "apellido", "Usuario",
                    "email", "nuevo.unico@test.com", "documento", "7777777",
                    "estado", "ACTIVO"));

            mockMvc.perform(post(URL_CREAR)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.email", is("nuevo.unico@test.com")));
        }

        @Test
        @DisplayName("IT-13 · Crear con email ya existente → 422")
        void crearEmailDuplicado_retorna422() throws Exception {
            String body = mapper.writeValueAsString(Map.of(
                    "nombre", "Copia", "apellido", "Ana",
                    "email", "ana.gomez@email.com",   // ya existe en el seeder
                    "documento", "6666666", "estado", "ACTIVO"));

            mockMvc.perform(post(URL_CREAR)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message",
                            containsString("Ya existe un cliente con este email")));
        }

        @Test
        @DisplayName("IT-14 · Editar cliente usando su propio email → 200 (no conflicto)")
        void editarMismoEmail_retorna200() throws Exception {
            String body = mapper.writeValueAsString(Map.of(
                    "nombre", "Ana", "apellido", "Gómez",
                    "email", "ana.gomez@email.com",   // mismo email del cliente id=1
                    "documento", "1000001", "estado", "ACTIVO"));

            mockMvc.perform(put(URL_EDITAR, 1)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("IT-15 · Editar cliente con email de otro cliente → 422")
        void editarEmailOcupado_retorna422() throws Exception {
            String body = mapper.writeValueAsString(Map.of(
                    "nombre", "Carlos", "apellido", "Pérez",
                    "email", "ana.gomez@email.com",   // email de cliente id=1, no del id=2
                    "documento", "1000002", "estado", "ACTIVO"));

            mockMvc.perform(put(URL_EDITAR, 2)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message",
                            containsString("Ya existe un cliente con este email")));
        }
    }
}