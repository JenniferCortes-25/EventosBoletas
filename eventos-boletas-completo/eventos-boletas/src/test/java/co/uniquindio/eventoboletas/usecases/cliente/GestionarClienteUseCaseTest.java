package co.uniquindio.eventoboletas.usecases.cliente;

import co.uniquindio.eventoboletas.application.dtos.request.ClienteRequest;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.usecases.cliente.GestionarClienteUseCase;
import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del CU-02: Gestionar Cliente.
 *
 * Cubre:
 *   CA-06  Flujo feliz — crear cliente con datos válidos
 *   CA-07  Email duplicado al crear (RN-07)
 *   CA-08  Email duplicado al editar (RN-07)
 *   CA-09  Eliminar cliente con boletos activos (RN-06)
 *   CA-10  Eliminar cliente sin boletos — flujo feliz
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CU-02 · GestionarClienteUseCase")
class GestionarClienteUseCaseTest {

    @Mock private ClienteRepository clienteRepository;

    @InjectMocks
    private GestionarClienteUseCase sut;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClienteRequest requestValido() {
        return new ClienteRequest("Juan", "Prueba", "juan.prueba@test.com",
                "9999999", EstadoCliente.ACTIVO);
    }

    private Cliente clientePersistido() {
        return Cliente.reconstituir(99L, "Juan", "Prueba",
                "juan.prueba@test.com", "9999999", EstadoCliente.ACTIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-06 — Flujo feliz: crear cliente
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-06 — Crear cliente con datos válidos
     *
     * Given: El email "juan.prueba@test.com" no existe en el sistema
     * When:  crear(request con datos válidos)
     * Then:  Retorna ClienteResponse con el ID asignado
     *        Y clienteRepository.guardar() fue invocado exactamente una vez
     */
    @Test
    @DisplayName("CA-06 · Flujo feliz — crear cliente con email nuevo")
    void ca06_crearCliente_exitoso() {
        // GIVEN
        when(clienteRepository.buscarPorEmail("juan.prueba@test.com"))
                .thenReturn(Optional.empty());
        when(clienteRepository.guardar(any(Cliente.class)))
                .thenReturn(clientePersistido());

        // WHEN
        ClienteResponse respuesta = sut.crear(requestValido());

        // THEN
        assertThat(respuesta).isNotNull();
        assertThat(respuesta.id()).isEqualTo(99L);
        assertThat(respuesta.email()).isEqualTo("juan.prueba@test.com");

        verify(clienteRepository, times(1)).guardar(any(Cliente.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-07 — Email duplicado al crear (RN-07)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-07 — Email duplicado en creación
     *
     * Given: Ya existe un cliente con email "ana.gomez@email.com"
     * When:  crear(request con ese mismo email)
     * Then:  422 — ReglaDeNegocioException "Ya existe un cliente con este email"
     *        Y clienteRepository.guardar() nunca fue invocado
     */
    @Test
    @DisplayName("CA-07 · Email duplicado al crear → 422 RN-07")
    void ca07_emailDuplicado_alCrear_lanzaExcepcion() {
        // GIVEN
        String emailDuplicado = "ana.gomez@email.com";
        Cliente existente = Cliente.reconstituir(1L, "Ana", "Gómez",
                emailDuplicado, "1000001", EstadoCliente.ACTIVO);
        when(clienteRepository.buscarPorEmail(emailDuplicado))
                .thenReturn(Optional.of(existente));

        ClienteRequest request = new ClienteRequest(
                "Nuevo", "Usuario", emailDuplicado, "8888888", EstadoCliente.ACTIVO);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.crear(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe un cliente con este email");

        verify(clienteRepository, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-08 — Email duplicado al editar (RN-07)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-08 — Email duplicado en edición
     *
     * Given: El cliente id=2 (Carlos Pérez) intenta cambiar su email
     *        Y ese email ya lo usa otro cliente (id=1)
     * When:  editar(2, request con email ocupado)
     * Then:  422 — ReglaDeNegocioException "Ya existe un cliente con este email"
     *        Y clienteRepository.guardar() nunca fue invocado
     */
    @Test
    @DisplayName("CA-08 · Email duplicado al editar → 422 RN-07")
    void ca08_emailDuplicado_alEditar_lanzaExcepcion() {
        // GIVEN
        Long id = 2L;
        String emailOcupado = "ana.gomez@email.com";

        Cliente clienteEditando = Cliente.reconstituir(id, "Carlos", "Pérez",
                "carlos.perez@email.com", "1000002", EstadoCliente.ACTIVO);
        when(clienteRepository.buscarPorId(id)).thenReturn(Optional.of(clienteEditando));
        when(clienteRepository.existeEmailEnOtroCliente(emailOcupado, id)).thenReturn(true);

        ClienteRequest request = new ClienteRequest(
                "Carlos", "Pérez", emailOcupado, "1000002", EstadoCliente.ACTIVO);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.editar(id, request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe un cliente con este email");

        verify(clienteRepository, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-09 — Eliminar cliente con boletos activos (RN-06)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-09 — Eliminar cliente con boletos PAGADOS
     *
     * Given: El cliente id=1 tiene boletos con estado PAGADO
     * When:  eliminar(1)
     * Then:  422 — ReglaDeNegocioException "No se puede eliminar un cliente con boletos activos"
     *        Y clienteRepository.eliminar() nunca fue invocado
     */
    @Test
    @DisplayName("CA-09 · Eliminar cliente con boletos activos → 422 RN-06")
    void ca09_eliminarClienteConBoletos_lanzaExcepcion() {
        // GIVEN
        Cliente cliente = Cliente.reconstituir(1L, "Ana", "Gómez",
                "ana.gomez@email.com", "1000001", EstadoCliente.ACTIVO);
        when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.tieneBoletosPagados(1L)).thenReturn(true);

        // WHEN + THEN
        assertThatThrownBy(() -> sut.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No se puede eliminar un cliente con boletos activos");

        verify(clienteRepository, never()).eliminar(anyLong());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CA-10 — Eliminar cliente sin boletos (flujo feliz)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CA-10 — Eliminar cliente sin boletos
     *
     * Given: El cliente id=12 (Sergio Medina) no tiene boletos
     * When:  eliminar(12)
     * Then:  No lanza excepción
     *        Y clienteRepository.eliminar(12) fue invocado exactamente una vez
     */
    @Test
    @DisplayName("CA-10 · Flujo feliz — eliminar cliente sin boletos")
    void ca10_eliminarClienteSinBoletos_exitoso() {
        // GIVEN
        Cliente cliente = Cliente.reconstituir(12L, "Sergio", "Medina",
                "sergio.med@email.com", "1000012", EstadoCliente.ACTIVO);
        when(clienteRepository.buscarPorId(12L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.tieneBoletosPagados(12L)).thenReturn(false);

        // WHEN + THEN — no lanza excepción
        assertThatCode(() -> sut.eliminar(12L)).doesNotThrowAnyException();

        verify(clienteRepository, times(1)).eliminar(12L);
    }
}
