package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Entidad de dominio Zona.
 *
 * RN-03: cupoDisponible > 0 para confirmar compra.
 * RN-04: precioFinal = precioBase × (1 + recargoPorcentaje) — calculado en dominio.
 *
 * Invariante: cupoDisponible no puede ser negativo ni superar cupoTotal.
 */
@Getter
@NoArgsConstructor
public class Zona {

    private Long id;
    private String nombre;
    private BigDecimal precioBase;
    private BigDecimal recargoPorcentaje;
    private int cupoTotal;
    private int cupoDisponible;
    private Long eventoId;

    private Zona(Long id, String nombre, BigDecimal precioBase,
                 BigDecimal recargoPorcentaje, int cupoTotal, int cupoDisponible, Long eventoId) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "El nombre de zona es obligatorio");
        this.precioBase = Objects.requireNonNull(precioBase);
        this.recargoPorcentaje = recargoPorcentaje != null ? recargoPorcentaje : BigDecimal.ZERO;
        this.cupoTotal = cupoTotal;
        this.cupoDisponible = cupoDisponible;
        this.eventoId = eventoId;
        validarInvariantes();
    }

    public static Zona crear(String nombre, BigDecimal precioBase,
                             BigDecimal recargoPorcentaje, int cupoTotal, Long eventoId) {
        return new Zona(null, nombre, precioBase, recargoPorcentaje, cupoTotal, cupoTotal, eventoId);
    }

    public static Zona reconstituir(Long id, String nombre, BigDecimal precioBase,
                                    BigDecimal recargoPorcentaje, int cupoTotal,
                                    int cupoDisponible, Long eventoId) {
        return new Zona(id, nombre, precioBase, recargoPorcentaje, cupoTotal, cupoDisponible, eventoId);
    }

    /**
     * RN-04: El precio final se calcula siempre en el dominio (servidor).
     * No puede ser modificado manualmente desde el exterior.
     */
    public BigDecimal calcularPrecioFinal() {
        return precioBase.multiply(BigDecimal.ONE.add(recargoPorcentaje))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * RN-03: Verifica que haya cupo antes de confirmar.
     */
    public void verificarCupoDisponible() {
        if (this.cupoDisponible <= 0) {
            throw new ReglaDeNegocioException(
                "No hay boletos disponibles para esta zona."
            );
        }
    }

    /**
     * Reduce el cupo en 1 al emitir un boleto.
     * Invariante: cupoDisponible >= 0.
     */
    public void reducirCupo() {
        verificarCupoDisponible();
        this.cupoDisponible--;
    }

    public void asignarId(Long id) {
        this.id = id;
    }

    private void validarInvariantes() {
        if (cupoDisponible < 0) throw new IllegalArgumentException("El cupo disponible no puede ser negativo");
        if (cupoDisponible > cupoTotal) throw new IllegalArgumentException("El cupo disponible no puede superar el cupo total");
        if (precioBase.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El precio base debe ser positivo");
    }
}
