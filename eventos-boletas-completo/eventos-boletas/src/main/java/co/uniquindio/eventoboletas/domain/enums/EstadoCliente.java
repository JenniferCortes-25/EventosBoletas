package co.uniquindio.eventoboletas.domain.enums;

/**
 * Enum que representa el estado de un Cliente.
 * Principio SRP: responsabilidad única de definir los estados posibles del cliente.
 */
public enum EstadoCliente {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}
