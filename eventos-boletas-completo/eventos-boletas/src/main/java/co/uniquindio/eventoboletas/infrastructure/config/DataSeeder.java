package co.uniquindio.eventoboletas.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Patrón Observer — Sujeto (Subject).
 * Al arrancar, publica AplicacionIniciadaEvent.
 * Los observadores (ClienteSeeder, EventoSeeder) reaccionan de forma desacoplada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void run(ApplicationArguments args) {
        log.info("DataSeeder: publicando evento de inicio...");
        eventPublisher.publishEvent(new AplicacionIniciadaEvent(this));
    }
}