package co.uniquindio.eventoboletas.domain.repositories;

import co.uniquindio.eventoboletas.domain.entities.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) del dominio para la entidad Cliente.
 * Principio DIP: el dominio define la interfaz; la infraestructura la implementa.
 * La capa de dominio NO depende de JPA ni Oracle.
 */
public interface ClienteRepository {

    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(Long id);

    Optional<Cliente> buscarPorEmail(String email);

    Optional<Cliente> buscarPorDocumento(String documento);

    List<Cliente> buscarTodos(int pagina, int tamano);

    List<Cliente> buscarPorNombreODocumento(String query);

    boolean existeEmailEnOtroCliente(String email, Long idExcluido);

    boolean tieneBoletosPagados(Long clienteId);

    void eliminar(Long clienteId);

    long contarTodos();
}
