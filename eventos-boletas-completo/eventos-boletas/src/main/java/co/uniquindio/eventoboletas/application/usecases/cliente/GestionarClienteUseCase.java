package co.uniquindio.eventoboletas.application.usecases.cliente;

import co.uniquindio.eventoboletas.application.dtos.request.ClienteRequest;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.dtos.response.PagedResponse;
import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de Uso CU-02: Gestionar Cliente (CRUD completo).
 *
 * Reglas de negocio validadas:
 * - RN-06: no eliminar clientes con boletos pagados.
 * - RN-07: email único en el sistema.
 *
 * Principios SOLID:
 * - SRP: sólo gestiona el ciclo de vida de Cliente.
 * - DIP: depende del puerto ClienteRepository, no de JPA.
 *
 * Patrón Command implícito: cada operación (crear, editar, eliminar) es un comando.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GestionarClienteUseCase {

    private final ClienteRepository clienteRepository;

    // ── LISTAR ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ClienteResponse> listar(int pagina, int tamano) {
        List<ClienteResponse> clientes = clienteRepository.buscarTodos(pagina, tamano)
            .stream().map(this::toResponse).toList();
        long total = clienteRepository.contarTodos();
        return PagedResponse.of(clientes, pagina, tamano, total);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return clienteRepository.buscarPorId(id)
            .map(this::toResponse)
            .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", id));
    }

    // ── CREAR ─────────────────────────────────────────────────────────────────

    /**
     * RF-07 + RN-07: crea cliente validando email único.
     */
    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        // RN-07: email único
        if (clienteRepository.buscarPorEmail(request.email()).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe un cliente con este email.");
        }

        // Factory Method del dominio garantiza estado ACTIVO y email válido
        // Crea el cliente y luego aplica el estado enviado desde el frontend
        Cliente cliente = Cliente.crear(
            request.nombre(), request.apellido(),
            request.email(), request.documento()
        );
        cliente.actualizarDatos(
            request.nombre(), request.apellido(),
            request.email(), request.documento(), request.estado()
        );

        Cliente persistido = clienteRepository.guardar(cliente);
        log.info("Cliente creado: id={}, email={}", persistido.getId(), persistido.getEmail());
        return toResponse(persistido);
    }

    // ── EDITAR ────────────────────────────────────────────────────────────────

    /**
     * RF-08 + RN-07: actualiza cliente validando email único en otro registro.
     */
    @Transactional
    public ClienteResponse editar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.buscarPorId(id)
            .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", id));

        // RN-07: email único excluyendo al propio cliente
        if (clienteRepository.existeEmailEnOtroCliente(request.email(), id)) {
            throw new ReglaDeNegocioException("Ya existe un cliente con este email.");
        }

        cliente.actualizarDatos(
            request.nombre(), request.apellido(),
            request.email(), request.documento(), request.estado()
        );

        Cliente actualizado = clienteRepository.guardar(cliente);
        log.info("Cliente actualizado: id={}", id);
        return toResponse(actualizado);
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────────

    /**
     * RF-09 + RN-06: elimina cliente sólo si no tiene boletos pagados.
     */
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.buscarPorId(id)
            .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", id));

        boolean tieneBoletos = clienteRepository.tieneBoletosPagados(id);
        // La regla está en el dominio — principio DRY y Rich Domain Model
        cliente.verificarEliminable(tieneBoletos); // RN-06

        clienteRepository.eliminar(id);
        log.info("Cliente eliminado: id={}", id);
    }

    // ── Mapper interno ────────────────────────────────────────────────────────
    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getApellido(),
                                   c.getEmail(), c.getDocumento(), c.getEstado());
    }
}
