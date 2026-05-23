package co.uniquindio.eventoboletas.application.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.dtos.response.EventoResponse;
import co.uniquindio.eventoboletas.application.dtos.response.ZonaResponse;
import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.entities.Evento;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import co.uniquindio.eventoboletas.domain.repositories.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso auxiliar para búsquedas durante la transacción de compra.
 * Separa la responsabilidad de consulta de la responsabilidad de escritura (CQRS light).
 * Principio SRP: sólo consulta, no muta estado.
 */
@Service
@RequiredArgsConstructor
public class BuscarParaTransaccionUseCase {

    private final ClienteRepository clienteRepository;
    private final EventoRepository  eventoRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientes(String query) {
        return clienteRepository.buscarPorNombreODocumento(query).stream()
            .map(this::toClienteResponse)
            .toList();
    }

    /** Solo ACTIVOS — usado internamente si se necesita filtrar. */
    @Transactional(readOnly = true)
    public List<EventoResponse> buscarEventosActivos() {
        return eventoRepository.buscarActivos().stream()
            .map(this::toEventoResponse)
            .toList();
    }

    // ── NUEVO ─────────────────────────────────────────────────────────────────
    /**
     * Todos los eventos (ACTIVO, CANCELADO, AGOTADO) para la pantalla de compra.
     * El front-end usa el campo {@code estado} para:
     *  - ACTIVO   → seleccionable normalmente.
     *  - CANCELADO → visible pero completamente bloqueado (no se puede seleccionar).
     *  - AGOTADO  → zonas seleccionables pero con aviso inline de agotamiento
     *               cuando cupoDisponible == 0 en esa zona específica.
     */
    @Transactional(readOnly = true)
    public List<EventoResponse> listarTodosParaVista() {
        return eventoRepository.buscarTodos().stream()
            .map(this::toEventoResponse)
            .toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ClienteResponse toClienteResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getApellido(),
                                   c.getEmail(), c.getDocumento(), c.getEstado());
    }

    private EventoResponse toEventoResponse(Evento e) {
        List<ZonaResponse> zonas = e.getZonas().stream()
            .map(z -> new ZonaResponse(
                z.getId(), z.getNombre(), z.getPrecioBase(),
                z.getRecargoPorcentaje(), z.calcularPrecioFinal(),
                z.getCupoTotal(), z.getCupoDisponible()))
            .toList();
        return new EventoResponse(e.getId(), e.getNombre(), e.getFecha(),
                                  e.getLugar(), e.getEstado(), zonas);
    }
}