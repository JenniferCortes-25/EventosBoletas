package co.uniquindio.eventoboletas.infrastructure.persistence.repositories;

import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ClienteJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para ClienteJpa.
 * Patrón Repository: abstrae el acceso a datos y lo desacopla de la lógica de negocio.
 */
public interface ClienteJpaRepository extends JpaRepository<ClienteJpa, Long> {

    Optional<ClienteJpa> findByEmail(String email);

    Optional<ClienteJpa> findByDocumento(String documento);

    @Query("""
        SELECT c FROM ClienteJpa c
        WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
           OR c.documento LIKE CONCAT('%', :q, '%')
        """)
    List<ClienteJpa> buscarPorNombreODocumento(@Param("q") String q);

    @Query("SELECT COUNT(c) > 0 FROM ClienteJpa c WHERE c.email = :email AND c.id <> :idExcluido")
    boolean existeEmailEnOtroCliente(@Param("email") String email, @Param("idExcluido") Long idExcluido);

    @Query("SELECT COUNT(b) > 0 FROM BoletoJpa b WHERE b.cliente.id = :clienteId AND b.estado = :estado")
    boolean tieneBoletos(@Param("clienteId") Long clienteId, @Param("estado") EstadoBoleto estado);
}
