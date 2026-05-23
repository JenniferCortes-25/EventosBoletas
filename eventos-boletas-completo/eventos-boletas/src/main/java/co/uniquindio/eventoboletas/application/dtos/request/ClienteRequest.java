package co.uniquindio.eventoboletas.application.dtos.request;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para crear/editar un Cliente.
 * Principio SRP: sólo transporta datos de la capa de presentación a la aplicación.
 */
public record ClienteRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    String apellido,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ingrese un email válido.")
    String email,

    @NotBlank(message = "El documento es obligatorio")
    String documento,

    @NotNull(message = "El estado es obligatorio")
    EstadoCliente estado
) {}
