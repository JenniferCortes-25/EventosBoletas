package co.uniquindio.eventoboletas.application.dtos.response;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 * Patrón Generic: reutilizable para cualquier entidad.
 */
public record PagedResponse<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas
) {
    public static <T> PagedResponse<T> of(List<T> contenido, int pagina, int tamano, long total) {
        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return new PagedResponse<>(contenido, pagina, tamano, total, totalPaginas);
    }
}
