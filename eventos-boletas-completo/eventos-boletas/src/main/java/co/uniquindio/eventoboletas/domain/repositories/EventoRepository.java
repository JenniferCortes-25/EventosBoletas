package co.uniquindio.eventoboletas.domain.repositories;

import co.uniquindio.eventoboletas.domain.entities.Evento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para Evento.
 */
public interface EventoRepository {

    Evento guardar(Evento evento);

    Optional<Evento> buscarPorId(Long id);

    /** Solo los ACTIVOS — usado por la validación de compra (RN-02). */
    List<Evento> buscarActivos();

    // ── NUEVO ─────────────────────────────────────────────────────────────────
    /**
     * Todos los eventos (ACTIVO, CANCELADO, AGOTADO) con sus zonas,
     * ordenados por fecha. Usado en la pantalla de compra para mostrar
     * eventos cancelados (no seleccionables) y agotados (zona bloqueada).
     */
    List<Evento> buscarTodos();
}