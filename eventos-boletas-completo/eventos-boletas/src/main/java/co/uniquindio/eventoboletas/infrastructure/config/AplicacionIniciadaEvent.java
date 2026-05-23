package co.uniquindio.eventoboletas.infrastructure.config;

import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado al arrancar la aplicación.
 * Patrón Observer — sujeto: DataSeeder, observadores: los seeders de cada entidad.
 */
public class AplicacionIniciadaEvent extends ApplicationEvent {
    public AplicacionIniciadaEvent(Object source) {
        super(source);
    }
}