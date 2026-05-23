package co.uniquindio.eventoboletas.infrastructure.config;

import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.EventoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.EventoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Patrón Observer — Observador 2.
 * Escucha AplicacionIniciadaEvent y carga los eventos y zonas de prueba.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventoSeeder {

    private final EventoJpaRepository eventoRepo;

    @EventListener
    @Transactional
    public void onAplicacionIniciada(AplicacionIniciadaEvent event) {
        if (eventoRepo.count() > 0) {
            log.info("EventoSeeder: ya hay eventos, omitiendo.");
            return;
        }

        EventoJpa festival = EventoJpa.builder()
            .nombre("Festival Latinoamericano de Música")
            .fecha(LocalDateTime.now().plusDays(30))
            .lugar("Estadio El Campín, Bogotá")
            .estado(EstadoEvento.ACTIVO).build();
        festival.setZonas(List.of(
            zona("VIP",     new BigDecimal("350000"), new BigDecimal("0.10"), 50,  festival),
            zona("General", new BigDecimal("120000"), new BigDecimal("0.05"), 200, festival),
            zona("Palco",   new BigDecimal("500000"), new BigDecimal("0.15"), 20,  festival)
        ));

        EventoJpa conferencia = EventoJpa.builder()
            .nombre("UniQuindío Tech Conference 2026")
            .fecha(LocalDateTime.now().plusDays(15))
            .lugar("Auditorio UniQuindío, Armenia")
            .estado(EstadoEvento.ACTIVO).build();
        conferencia.setZonas(List.of(
            zona("Premium", new BigDecimal("80000"), new BigDecimal("0.08"), 100, conferencia),
            zona("Libre",   new BigDecimal("30000"), new BigDecimal("0.00"), 300, conferencia)
        ));

        EventoJpa teatro = EventoJpa.builder()
            .nombre("Hamlet — Compañía Nacional de Teatro")
            .fecha(LocalDateTime.now().plusDays(7))
            .lugar("Teatro Quindío, Armenia")
            .estado(EstadoEvento.ACTIVO).build();
        teatro.setZonas(List.of(
            zona("Butaca Preferencial", new BigDecimal("95000"), new BigDecimal("0.05"), 80, teatro),
            ZonaJpa.builder().nombre("Galería").precioBase(new BigDecimal("40000"))
                .recargoPorcentaje(BigDecimal.ZERO).cupoTotal(50).cupoDisponible(0).evento(teatro).build()
        ));

        EventoJpa cancelado = EventoJpa.builder()
            .nombre("Concierto Cancelado — Artista Internacional")
            .fecha(LocalDateTime.now().plusDays(5))
            .lugar("Plaza de Bolívar, Armenia")
            .estado(EstadoEvento.CANCELADO).build();

        eventoRepo.saveAll(List.of(festival, conferencia, teatro, cancelado));
        log.info("EventoSeeder: 4 eventos cargados.");
    }

    private ZonaJpa zona(String nombre, BigDecimal precio, BigDecimal recargo,
                          int cupo, EventoJpa evento) {
        return ZonaJpa.builder()
            .nombre(nombre).precioBase(precio).recargoPorcentaje(recargo)
            .cupoTotal(cupo).cupoDisponible(cupo).evento(evento).build();
    }
}