package co.uniquindio.eventoboletas.infrastructure.config;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ClienteJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.EventoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ClienteJpaRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.EventoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cargador de datos de prueba al arranque.
 * ≥ 10 clientes, ≥ 20 boletos (via zonas con cupo).
 *
 * Patrón Observer: implementa ApplicationRunner, Spring lo invoca automáticamente.
 * Sólo carga datos si la base de datos está vacía (idempotente).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ClienteJpaRepository clienteRepo;
    private final EventoJpaRepository  eventoRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clienteRepo.count() > 0) {
            log.info("La base de datos ya tiene datos. Omitiendo seed.");
            return;
        }
        log.info("Cargando datos de prueba...");
        cargarClientes();
        cargarEventos();
        log.info("Datos de prueba cargados exitosamente.");
    }

    private void cargarClientes() {
        List<ClienteJpa> clientes = List.of(
            cliente("Ana",       "Gómez",    "ana.gomez@email.com",     "1000001", EstadoCliente.ACTIVO),
            cliente("Carlos",    "Pérez",    "carlos.perez@email.com",  "1000002", EstadoCliente.ACTIVO),
            cliente("Valentina", "Torres",   "vale.torres@email.com",   "1000003", EstadoCliente.ACTIVO),
            cliente("Andrés",    "López",    "andres.lopez@email.com",  "1000004", EstadoCliente.ACTIVO),
            cliente("María",     "Ramírez",  "maria.ramirez@email.com", "1000005", EstadoCliente.ACTIVO),
            cliente("Felipe",    "Castro",   "felipe.castro@email.com", "1000006", EstadoCliente.ACTIVO),
            cliente("Laura",     "Morales",  "laura.morales@email.com", "1000007", EstadoCliente.ACTIVO),
            cliente("Juan",      "Herrera",  "juan.herrera@email.com",  "1000008", EstadoCliente.ACTIVO),
            cliente("Sofía",     "Vargas",   "sofia.vargas@email.com",  "1000009", EstadoCliente.INACTIVO),
            cliente("Miguel",    "Jiménez",  "miguel.jm@email.com",     "1000010", EstadoCliente.BLOQUEADO),
            cliente("Lucía",     "Navarro",  "lucia.nav@email.com",     "1000011", EstadoCliente.ACTIVO),
            cliente("Sergio",    "Medina",   "sergio.med@email.com",    "1000012", EstadoCliente.ACTIVO)
        );
        clienteRepo.saveAll(clientes);
        log.info("{} clientes cargados", clientes.size());
    }

    private void cargarEventos() {
        // Evento 1 — Festival de música (activo, con 3 zonas)
        EventoJpa festival = EventoJpa.builder()
            .nombre("Festival Latinoamericano de Música")
            .fecha(LocalDateTime.now().plusDays(30))
            .lugar("Estadio El Campín, Bogotá")
            .estado(EstadoEvento.ACTIVO)
            .build();

        ZonaJpa vip1     = zona("VIP",     new BigDecimal("350000"), new BigDecimal("0.10"), 50,  festival);
        ZonaJpa general1 = zona("General", new BigDecimal("120000"), new BigDecimal("0.05"), 200, festival);
        ZonaJpa palco1   = zona("Palco",   new BigDecimal("500000"), new BigDecimal("0.15"), 20,  festival);
        festival.setZonas(List.of(vip1, general1, palco1));

        // Evento 2 — Conferencia tech (activo, con 2 zonas)
        EventoJpa conferencia = EventoJpa.builder()
            .nombre("UniQuindío Tech Conference 2026")
            .fecha(LocalDateTime.now().plusDays(15))
            .lugar("Auditorio UniQuindío, Armenia")
            .estado(EstadoEvento.ACTIVO)
            .build();

        ZonaJpa premium2 = zona("Premium",  new BigDecimal("80000"),  new BigDecimal("0.08"), 100, conferencia);
        ZonaJpa libre2   = zona("Libre",    new BigDecimal("30000"),  new BigDecimal("0.00"), 300, conferencia);
        conferencia.setZonas(List.of(premium2, libre2));

        // Evento 3 — Teatro (activo, 2 zonas, una agotada)
        EventoJpa teatro = EventoJpa.builder()
            .nombre("Hamlet — Compañía Nacional de Teatro")
            .fecha(LocalDateTime.now().plusDays(7))
            .lugar("Teatro Quindío, Armenia")
            .estado(EstadoEvento.ACTIVO)
            .build();

        ZonaJpa butaca3 = zona("Butaca Preferencial", new BigDecimal("95000"), new BigDecimal("0.05"), 80, teatro);
        ZonaJpa agotada3 = ZonaJpa.builder()
            .nombre("Galería").precioBase(new BigDecimal("40000"))
            .recargoPorcentaje(new BigDecimal("0.00"))
            .cupoTotal(50).cupoDisponible(0).evento(teatro).build();
        teatro.setZonas(List.of(butaca3, agotada3));

        // Evento 4 — Cancelado (para probar RN-02)
        EventoJpa cancelado = EventoJpa.builder()
            .nombre("Concierto Cancelado — Artista Internacional")
            .fecha(LocalDateTime.now().plusDays(5))
            .lugar("Plaza de Bolívar, Armenia")
            .estado(EstadoEvento.CANCELADO)
            .build();

        eventoRepo.saveAll(List.of(festival, conferencia, teatro, cancelado));
        log.info("4 eventos y {} zonas cargados", 3 + 2 + 2 + 0);
    }

    private ClienteJpa cliente(String nombre, String apellido, String email,
                                String doc, EstadoCliente estado) {
        return ClienteJpa.builder()
            .nombre(nombre).apellido(apellido).email(email)
            .documento(doc).estado(estado).build();
    }

    private ZonaJpa zona(String nombre, BigDecimal precioBase, BigDecimal recargo,
                          int cupo, EventoJpa evento) {
        return ZonaJpa.builder()
            .nombre(nombre).precioBase(precioBase)
            .recargoPorcentaje(recargo)
            .cupoTotal(cupo).cupoDisponible(cupo)
            .evento(evento).build();
    }
}
