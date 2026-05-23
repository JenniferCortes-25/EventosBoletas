package co.uniquindio.eventoboletas.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ZONAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_zona")
    @SequenceGenerator(name = "seq_zona", sequenceName = "SEQ_ZONAS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "PRECIO_BASE", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "RECARGO_PORCENTAJE", nullable = false, precision = 5, scale = 2)
    private BigDecimal recargoPorcentaje;

    @Column(name = "CUPO_TOTAL", nullable = false)
    private int cupoTotal;

    @Column(name = "CUPO_DISPONIBLE", nullable = false)
    private int cupoDisponible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENTO_ID", nullable = false)
    private EventoJpa evento;

    @OneToMany(mappedBy = "zona", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BoletoJpa> boletos = new ArrayList<>();
}
