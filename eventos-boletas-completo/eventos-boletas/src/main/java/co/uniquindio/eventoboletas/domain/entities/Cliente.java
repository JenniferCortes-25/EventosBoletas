package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad de dominio Cliente.
 *
 * Patrones aplicados:
 * - Rich Domain Model: la entidad protege sus invariantes internamente.
 * - Factory Method: método estático crear() garantiza construcción válida.
 *
 * Principios SOLID:
 * - SRP: sólo conoce sus propias reglas de negocio.
 * - OCP: abierto a extensión (nuevas reglas), cerrado a modificación del contrato.
 */
@Getter
@NoArgsConstructor
public class Cliente {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String documento;
    private EstadoCliente estado;
    private List<Boleto> boletos = new ArrayList<>();

    private Cliente(Long id, String nombre, String apellido,
                    String email, String documento, EstadoCliente estado) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "El nombre es obligatorio");
        this.apellido = Objects.requireNonNull(apellido, "El apellido es obligatorio");
        this.email = Objects.requireNonNull(email, "El email es obligatorio");
        this.documento = Objects.requireNonNull(documento, "El documento es obligatorio");
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
    }

    /**
     * Factory Method - garantiza construcción válida de la entidad (patrón Creacional).
     */
    public static Cliente crear(String nombre, String apellido,
                                String email, String documento) {
        validarEmail(email);
        return new Cliente(null, nombre, apellido, email, documento, EstadoCliente.ACTIVO);
    }

    public static Cliente reconstituir(Long id, String nombre, String apellido,
                                       String email, String documento, EstadoCliente estado) {
        return new Cliente(id, nombre, apellido, email, documento, estado);
    }

    /**
     * RN-01: El cliente debe tener estado ACTIVO para adquirir un boleto.
     */
    public void verificarHabilitadoParaComprar() {
        if (this.estado != EstadoCliente.ACTIVO) {
            throw new ReglaDeNegocioException(
                "El cliente no está habilitado para comprar. Estado actual: " + this.estado
            );
        }
    }

    /**
     * RN-06: No se puede eliminar un cliente con boletos en estado PAGADO.
     */
    public void verificarEliminable(boolean tieneBoletosPagados) {
        if (tieneBoletosPagados) {
            throw new ReglaDeNegocioException(
                "No se puede eliminar un cliente con boletos activos."
            );
        }
    }

    public void actualizarDatos(String nombre, String apellido,
                                String email, String documento, EstadoCliente estado) {
        validarEmail(email);
        this.nombre = Objects.requireNonNull(nombre);
        this.apellido = Objects.requireNonNull(apellido);
        this.email = email;
        this.documento = Objects.requireNonNull(documento);
        this.estado = Objects.requireNonNull(estado);
    }

    public void asignarId(Long id) {
        if (this.id != null) throw new IllegalStateException("El ID ya fue asignado");
        this.id = Objects.requireNonNull(id);
    }

    public List<Boleto> getBoletos() {
        return Collections.unmodifiableList(boletos);
    }

    private static void validarEmail(String email) {
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new ReglaDeNegocioException("Ingrese un email válido.");
        }
    }
}
