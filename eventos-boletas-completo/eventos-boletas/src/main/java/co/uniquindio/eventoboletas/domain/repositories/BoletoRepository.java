package co.uniquindio.eventoboletas.domain.repositories;

import co.uniquindio.eventoboletas.domain.entities.Boleto;

import java.util.Optional;

public interface BoletoRepository {

    Boleto guardar(Boleto boleto);

    Optional<Boleto> buscarPorId(Long id);

    boolean existeCodigoQR(String codigoQR);
}
