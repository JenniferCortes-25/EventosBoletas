package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Entidad de dominio Zona.
 *
 * RN-03: cupoDisponible >= cantidad solicitada para confirmar compra.
 * RN-04: precioFinal = precioBase × (1 + recargo_zona + recargo_metodoPago) — calculado en dominio.
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

    // -------------------------------------------------------------------------
    // RN-04 — Cálculo de precio
    // -------------------------------------------------------------------------

    /**
     * RN-04: precioFinal = precioBase × (1 + recargo_zona + recargo_metodoPago).
     * El recargo del método de pago se suma al recargo propio de la zona.
     * EFECTIVO tiene recargoPorcentaje = 0, por lo que no añade nada.
     * Calculado siempre en el dominio (servidor) — nunca en el cliente.
     */
    public BigDecimal calcularPrecioFinal(MetodoPago metodoPago) {
        BigDecimal recargoCombinado = this.recargoPorcentaje
                .add(metodoPago.getRecargoPorcentaje());
        return precioBase.multiply(BigDecimal.ONE.add(recargoCombinado))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sobrecarga de compatibilidad para código o tests que calculen sin método de pago.
     * Usa únicamente el recargo base de la zona (sin recargo de método de pago).
     */
    public BigDecimal calcularPrecioFinal() {
        return precioBase.multiply(BigDecimal.ONE.add(recargoPorcentaje))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // RN-03 — Validación y reducción de cupo
    // -------------------------------------------------------------------------

    /**
     * RN-03: Verifica que haya cupo suficiente para la cantidad solicitada.
     */
    public void verificarCupoDisponible(int cantidad) {
        if (this.cupoDisponible < cantidad) {
            throw new ReglaDeNegocioException(
                "No hay suficientes boletos disponibles para esta zona. " +
                "Disponibles: " + this.cupoDisponible + ", solicitados: " + cantidad + "."
            );
        }
    }

    /**
     * RN-03: Sobrecarga de compatibilidad — verifica cupo para 1 boleto.
     */
    public void verificarCupoDisponible() {
        verificarCupoDisponible(1);
    }

    /**
     * Reduce el cupo en {@code cantidad} unidades al emitir boletos.
     * Invariante: cupoDisponible >= 0.
     */
    public void reducirCupo(int cantidad) {
        verificarCupoDisponible(cantidad);
        this.cupoDisponible -= cantidad;
    }

    /**
     * Sobrecarga de compatibilidad — reduce cupo en 1.
     */
    public void reducirCupo() {
        reducirCupo(1);
    }

    public void asignarId(Long id) {
        this.id = id;
    }

    private void validarInvariantes() {
        if (cupoDisponible < 0)
            throw new IllegalArgumentException("El cupo disponible no puede ser negativo");
        if (cupoDisponible > cupoTotal)
            throw new IllegalArgumentException("El cupo disponible no puede superar el cupo total");
        if (precioBase.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El precio base debe ser positivo");
    }
}