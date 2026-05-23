package co.uniquindio.eventoboletas.infrastructure.adapters;

import co.uniquindio.eventoboletas.domain.entities.Boleto;
import co.uniquindio.eventoboletas.domain.entities.Pago;
import co.uniquindio.eventoboletas.domain.repositories.BoletoRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.BoletoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ClienteJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.PagoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.BoletoJpaRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ClienteJpaRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ZonaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador para BoletoRepository.
 * Orquesta la persistencia de Boleto y su Pago asociado (relación 1:1).
 */
@Component
@RequiredArgsConstructor
public class BoletoRepositoryAdapter implements BoletoRepository {

    private final BoletoJpaRepository   boletoJpaRepository;
    private final ClienteJpaRepository  clienteJpaRepository;
    private final ZonaJpaRepository     zonaJpaRepository;

    @Override
    public Boleto guardar(Boleto boleto) {
        ClienteJpa clienteRef = clienteJpaRepository.getReferenceById(boleto.getClienteId());
        ZonaJpa    zonaRef    = zonaJpaRepository.getReferenceById(boleto.getZonaId());

        BoletoJpa boletoJpa = BoletoJpa.builder()
            .id(boleto.getId())
            .codigoQR(boleto.getCodigoQR())
            .precioFinal(boleto.getPrecioFinal())
            .fechaEmision(boleto.getFechaEmision())
            .estado(boleto.getEstado())
            .cliente(clienteRef)
            .zona(zonaRef)
            .build();

        // Construir pago vinculado (relación bidireccional 1:1)
        Pago pago = boleto.getPago();
        PagoJpa pagoJpa = PagoJpa.builder()
            .id(pago.getId())
            .monto(pago.getMonto())
            .metodoPago(pago.getMetodoPago())
            .fechaPago(pago.getFechaPago())
            .estado(pago.getEstado())
            .boleto(boletoJpa)
            .build();

        boletoJpa.setPago(pagoJpa);

        BoletoJpa guardado = boletoJpaRepository.save(boletoJpa);
        return toDomain(guardado);
    }

    @Override
    public Optional<Boleto> buscarPorId(Long id) {
        return boletoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existeCodigoQR(String codigoQR) {
        return boletoJpaRepository.existsByCodigoQR(codigoQR);
    }

    private Boleto toDomain(BoletoJpa jpa) {
        Boleto b = Boleto.reconstituir(jpa.getId(), jpa.getCodigoQR(), jpa.getPrecioFinal(),
                                       jpa.getFechaEmision(), jpa.getEstado(),
                                       jpa.getCliente().getId(), jpa.getZona().getId());
        if (jpa.getPago() != null) {
            PagoJpa p = jpa.getPago();
            b.asignarPago(Pago.reconstituir(p.getId(), p.getMonto(), p.getMetodoPago(),
                                            p.getFechaPago(), p.getEstado()));
        }
        return b;
    }
}
