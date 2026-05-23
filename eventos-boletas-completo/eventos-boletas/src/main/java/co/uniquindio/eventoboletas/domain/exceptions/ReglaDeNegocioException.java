package co.uniquindio.eventoboletas.domain.exceptions;

/**
 * Excepción de dominio para violaciones de reglas de negocio.
 * Principio SRP: sólo representa errores de dominio, no de infraestructura.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }

    public ReglaDeNegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
