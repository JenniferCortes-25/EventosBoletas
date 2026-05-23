package co.uniquindio.eventoboletas.infrastructure.adapters;

import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ClienteJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ClienteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto de dominio ClienteRepository
 * usando Spring Data JPA + Oracle.
 *
 * Patrón Adapter (Estructural):
 * Convierte la interfaz del repositorio JPA a la interfaz esperada por el dominio.
 *
 * Principio DIP: el dominio sólo conoce ClienteRepository (puerto),
 * nunca esta clase concreta.
 */
@Component
@RequiredArgsConstructor
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpaRepository;

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteJpa jpa = toJpa(cliente);
        ClienteJpa guardado = jpaRepository.save(jpa);
        return toDomain(guardado);
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return jpaRepository.findByDocumento(documento).map(this::toDomain);
    }

    @Override
    public List<Cliente> buscarTodos(int pagina, int tamano) {
        return jpaRepository.findAll(PageRequest.of(pagina, tamano))
            .getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Cliente> buscarPorNombreODocumento(String query) {
        return jpaRepository.buscarPorNombreODocumento(query)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existeEmailEnOtroCliente(String email, Long idExcluido) {
        return jpaRepository.existeEmailEnOtroCliente(email, idExcluido);
    }

    @Override
    public boolean tieneBoletosPagados(Long clienteId) {
        return jpaRepository.tieneBoletos(clienteId, EstadoBoleto.PAGADO);
    }

    @Override
    public void eliminar(Long clienteId) {
        jpaRepository.deleteById(clienteId);
    }

    @Override
    public long contarTodos() {
        return jpaRepository.count();
    }

    // ── Mappers internos (dominio ↔ JPA) ──────────────────────────────────────

    private Cliente toDomain(ClienteJpa jpa) {
        return Cliente.reconstituir(
            jpa.getId(), jpa.getNombre(), jpa.getApellido(),
            jpa.getEmail(), jpa.getDocumento(), jpa.getEstado()
        );
    }

    private ClienteJpa toJpa(Cliente c) {
        return ClienteJpa.builder()
            .id(c.getId())
            .nombre(c.getNombre())
            .apellido(c.getApellido())
            .email(c.getEmail())
            .documento(c.getDocumento())
            .estado(c.getEstado())
            .build();
    }
}
