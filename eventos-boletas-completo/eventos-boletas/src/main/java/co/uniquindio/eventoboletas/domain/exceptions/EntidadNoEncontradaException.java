package co.uniquindio.eventoboletas.domain.exceptions;

public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException(String entidad, Long id) {
        super(entidad + " con id " + id + " no fue encontrado(a).");
    }

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
