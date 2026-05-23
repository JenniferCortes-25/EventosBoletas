package co.uniquindio.eventoboletas.infrastructure.persistence.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA de infraestructura para Cliente.
 *
 * Principio de separación de capas (Clean Architecture):
 * Esta clase es DISTINTA a la entidad de dominio Cliente.
 * La infraestructura NO contamina el dominio con anotaciones JPA.
 *
 * Patrón Adapter: se mapea hacia/desde la entidad de dominio mediante mappers.
 */
@Entity
@Table(name = "CLIENTES",
       uniqueConstraints = @UniqueConstraint(name = "UQ_CLIENTE_EMAIL", columnNames = "EMAIL"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cliente")
    @SequenceGenerator(name = "seq_cliente", sequenceName = "SEQ_CLIENTES",
                       allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "APELLIDO", nullable = false, length = 100)
    private String apellido;

    @Column(name = "EMAIL", nullable = false, length = 150)
    private String email;

    @Column(name = "DOCUMENTO", nullable = false, length = 20)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoCliente estado;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoletoJpa> boletos = new ArrayList<>();
}