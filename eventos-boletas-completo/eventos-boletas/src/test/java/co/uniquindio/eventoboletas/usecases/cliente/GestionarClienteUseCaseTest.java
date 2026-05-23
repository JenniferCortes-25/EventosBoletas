package co.uniquindio.eventoboletas.usecases.cliente;

import co.uniquindio.eventoboletas.application.dtos.request.ClienteRequest;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.usecases.cliente.GestionarClienteUseCase;
import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CU-02 · GestionarClienteUseCase — Tests unitarios con Mockito")
class GestionarClienteUseCaseTest {

    @Mock private ClienteRepository clienteRepository;
    @InjectMocks private GestionarClienteUseCase sut;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClienteRequest requestValido() {
        return new ClienteRequest("Juan", "Prueba", "juan@test.com",
                "9999999", EstadoCliente.ACTIVO);
    }

    private Cliente clientePersistido() {
        return Cliente.reconstituir(99L, "Juan", "Prueba",
                "juan@test.com", "9999999", EstadoCliente.ACTIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-06 — No eliminar cliente con boletos pagados
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-06 · No eliminar cliente con boletos PAGADOS")
    class Rn06 {

        @Test
        @DisplayName("Cliente sin boletos → eliminación exitosa, eliminar() invocado una vez")
        void sinBoletos_eliminaCorrectamente() {
            Cliente cliente = Cliente.reconstituir(12L, "Sergio", "Medina",
                    "sergio@test.com", "1000012", EstadoCliente.ACTIVO);
            when(clienteRepository.buscarPorId(12L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.tieneBoletosPagados(12L)).thenReturn(false);

            assertThatCode(() -> sut.eliminar(12L)).doesNotThrowAnyException();
            verify(clienteRepository, times(1)).eliminar(12L);
        }

        @Test
        @DisplayName("Cliente con boletos PAGADOS → ReglaDeNegocioException, eliminar() nunca invocado")
        void conBoletosPagados_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(1L, "Ana", "Gómez",
                    "ana@test.com", "1000001", EstadoCliente.ACTIVO);
            when(clienteRepository.buscarPorId(1L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.tieneBoletosPagados(1L)).thenReturn(true);

            assertThatThrownBy(() -> sut.eliminar(1L))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("No se puede eliminar un cliente con boletos activos");

            verify(clienteRepository, never()).eliminar(anyLong());
        }

        @Test
        @DisplayName("Cliente inexistente → EntidadNoEncontradaException")
        void clienteNoExiste_lanzaEntidadNoEncontrada() {
            when(clienteRepository.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.eliminar(999L))
                    .isInstanceOf(EntidadNoEncontradaException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RN-07 — Email único
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RN-07 · Email único en el sistema")
    class Rn07 {

        @Test
        @DisplayName("Crear con email nuevo → retorna ClienteResponse con ID asignado")
        void crear_emailNuevo_exitoso() {
            when(clienteRepository.buscarPorEmail("juan@test.com")).thenReturn(Optional.empty());
            when(clienteRepository.guardar(any())).thenReturn(clientePersistido());

            ClienteResponse respuesta = sut.crear(requestValido());

            assertThat(respuesta.id()).isEqualTo(99L);
            assertThat(respuesta.email()).isEqualTo("juan@test.com");
            verify(clienteRepository, times(1)).guardar(any());
        }

        @Test
        @DisplayName("Crear con email duplicado → ReglaDeNegocioException, guardar() nunca invocado")
        void crear_emailDuplicado_lanzaExcepcion() {
            Cliente existente = Cliente.reconstituir(1L, "Ana", "Gómez",
                    "juan@test.com", "1000001", EstadoCliente.ACTIVO);
            when(clienteRepository.buscarPorEmail("juan@test.com"))
                    .thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> sut.crear(requestValido()))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("Ya existe un cliente con este email");

            verify(clienteRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Editar con email disponible → actualización exitosa")
        void editar_emailDisponible_exitoso() {
            Cliente cliente = Cliente.reconstituir(2L, "Carlos", "Pérez",
                    "carlos@test.com", "1000002", EstadoCliente.ACTIVO);
            when(clienteRepository.buscarPorId(2L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.existeEmailEnOtroCliente("nuevo@test.com", 2L)).thenReturn(false);
            when(clienteRepository.guardar(any())).thenReturn(
                    Cliente.reconstituir(2L, "Carlos", "Pérez", "nuevo@test.com", "1000002", EstadoCliente.ACTIVO));

            ClienteRequest request = new ClienteRequest("Carlos", "Pérez",
                    "nuevo@test.com", "1000002", EstadoCliente.ACTIVO);
            ClienteResponse respuesta = sut.editar(2L, request);

            assertThat(respuesta.email()).isEqualTo("nuevo@test.com");
            verify(clienteRepository, times(1)).guardar(any());
        }

        @Test
        @DisplayName("Editar con email ocupado por otro cliente → ReglaDeNegocioException")
        void editar_emailOcupado_lanzaExcepcion() {
            Cliente cliente = Cliente.reconstituir(2L, "Carlos", "Pérez",
                    "carlos@test.com", "1000002", EstadoCliente.ACTIVO);
            when(clienteRepository.buscarPorId(2L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.existeEmailEnOtroCliente("ana@test.com", 2L)).thenReturn(true);

            ClienteRequest request = new ClienteRequest("Carlos", "Pérez",
                    "ana@test.com", "1000002", EstadoCliente.ACTIVO);

            assertThatThrownBy(() -> sut.editar(2L, request))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("Ya existe un cliente con este email");

            verify(clienteRepository, never()).guardar(any());
        }
    }
}