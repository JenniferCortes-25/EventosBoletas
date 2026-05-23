package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad de dominio Evento.
 * Patrón Rich Domain Model: protege sus invariantes.
 * RN-02: sólo se venden boletos cuando el evento está ACTIVO.
 */
@Getter
@NoArgsConstructor
public class Evento {

    private Long id;
    private String nombre;
    private LocalDateTime fecha;
    private String lugar;
    private EstadoEvento estado;
    private List<Zona> zonas = new ArrayList<>();

    private Evento(Long id, String nombre, LocalDateTime fecha,
                   String lugar, EstadoEvento estado) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "El nombre del evento es obligatorio");
        this.fecha = Objects.requireNonNull(fecha, "La fecha del evento es obligatoria");
        this.lugar = Objects.requireNonNull(lugar, "El lugar del evento es obligatorio");
        this.estado = Objects.requireNonNull(estado, "El estado del evento es obligatorio");
    }

    public static Evento crear(String nombre, LocalDateTime fecha, String lugar) {
        return new Evento(null, nombre, fecha, lugar, EstadoEvento.ACTIVO);
    }

    public static Evento reconstituir(Long id, String nombre, LocalDateTime fecha,
                                      String lugar, EstadoEvento estado) {
        return new Evento(id, nombre, fecha, lugar, estado);
    }

    /**
     * RN-02: El evento debe estar ACTIVO para vender boletos.
     */
    public void verificarDisponibleParaVenta() {
        if (this.estado != EstadoEvento.ACTIVO) {
            throw new ReglaDeNegocioException(
                "Este evento no está disponible para la venta. Estado: " + this.estado
            );
        }
    }

    public void agregarZona(Zona zona) {
        this.zonas.add(Objects.requireNonNull(zona));
    }

    public void asignarId(Long id) {
        if (this.id != null) throw new IllegalStateException("El ID ya fue asignado");
        this.id = id;
    }

    public List<Zona> getZonas() {
        return Collections.unmodifiableList(zonas);
    }
}
