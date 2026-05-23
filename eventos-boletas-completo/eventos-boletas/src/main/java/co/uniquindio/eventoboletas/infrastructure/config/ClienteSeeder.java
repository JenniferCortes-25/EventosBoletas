package co.uniquindio.eventoboletas.infrastructure.config;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ClienteJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ClienteJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Patrón Observer — Observador 1.
 * Escucha AplicacionIniciadaEvent y carga los clientes de prueba.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteSeeder {

    private final ClienteJpaRepository clienteRepo;

    @EventListener
    @Transactional
    public void onAplicacionIniciada(AplicacionIniciadaEvent event) {
        if (clienteRepo.count() > 0) {
            log.info("ClienteSeeder: ya hay clientes, omitiendo.");
            return;
        }
        List<ClienteJpa> clientes = List.of(
            cliente("Ana",       "Gómez",   "ana.gomez@email.com",     "1000001", EstadoCliente.ACTIVO),
            cliente("Carlos",    "Pérez",   "carlos.perez@email.com",  "1000002", EstadoCliente.ACTIVO),
            cliente("Valentina", "Torres",  "vale.torres@email.com",   "1000003", EstadoCliente.ACTIVO),
            cliente("Andrés",    "López",   "andres.lopez@email.com",  "1000004", EstadoCliente.ACTIVO),
            cliente("María",     "Ramírez", "maria.ramirez@email.com", "1000005", EstadoCliente.ACTIVO),
            cliente("Felipe",    "Castro",  "felipe.castro@email.com", "1000006", EstadoCliente.ACTIVO),
            cliente("Laura",     "Morales", "laura.morales@email.com", "1000007", EstadoCliente.ACTIVO),
            cliente("Juan",      "Herrera", "juan.herrera@email.com",  "1000008", EstadoCliente.ACTIVO),
            cliente("Sofía",     "Vargas",  "sofia.vargas@email.com",  "1000009", EstadoCliente.INACTIVO),
            cliente("Miguel",    "Jiménez", "miguel.jm@email.com",     "1000010", EstadoCliente.BLOQUEADO),
            cliente("Lucía",     "Navarro", "lucia.nav@email.com",     "1000011", EstadoCliente.ACTIVO),
            cliente("Sergio",    "Medina",  "sergio.med@email.com",    "1000012", EstadoCliente.ACTIVO)
        );
        clienteRepo.saveAll(clientes);
        log.info("ClienteSeeder: {} clientes cargados.", clientes.size());
    }

    private ClienteJpa cliente(String nombre, String apellido, String email,
                                String doc, EstadoCliente estado) {
        return ClienteJpa.builder()
            .nombre(nombre).apellido(apellido).email(email)
            .documento(doc).estado(estado).build();
    }
}