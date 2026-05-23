package co.uniquindio.eventoboletas.infrastructure.web.handlers.chain;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Eslabón 1: maneja errores de validación Bean Validation (400). */
public class ValidacionHandler extends ExceptionHandlerLink {

    @Override
    protected boolean puedeAtender(Exception ex) {
        return ex instanceof MethodArgumentNotValidException;
    }

    @Override
    protected ResponseEntity<Map<String, Object>> atender(Exception ex) {
        MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
        Map<String, String> campos = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err ->
            campos.put(((FieldError) err).getField(), err.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Datos inválidos");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }
}