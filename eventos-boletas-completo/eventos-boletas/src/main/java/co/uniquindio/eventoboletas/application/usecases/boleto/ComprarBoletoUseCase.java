package co.uniquindio.eventoboletas.application.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.dtos.response.PagoResponse;
import co.uniquindio.eventoboletas.domain.entities.*;
import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Template Method — implementación concreta del CU-01.
 *
 * Hereda el algoritmo P1→P10 de AbstractCompraUseCase e implementa
 * cada paso con la lógica real de negocio (repositorios, dominio, JPA).
 *
 * Patrón Facade: desde el controlador se invoca solo ejecutar();
 * la orquestación interna queda oculta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprarBoletoUseCase extends AbstractCompraUseCase {

    private final ClienteRepository clienteRepository;
    private final EventoRepository  eventoRepository;
    private final ZonaRepository    zonaRepository;
    private final BoletoRepository  boletoRepository;

    @Override
    @Transactional
    public List<BoletoResponse> ejecutar(ComprarBoletoRequest request) {
        log.info("Iniciando compra: clienteId={}, eventoId={}, zonaId={}, cantidad={}",
                 request.clienteId(), request.eventoId(), request.zonaId(), request.cantidad());
        return super.ejecutar(request);
    }

    @Override
    protected Cliente obtenerCliente(Long clienteId) {
        return clienteRepository.buscarPorId(clienteId)
            .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", clienteId));
    }

    @Override
    protected void validarCliente(Cliente cliente) {
        cliente.verificarHabilitadoParaComprar(); // RN-01
    }

    @Override
    protected Evento obtenerEvento(Long eventoId) {
        return eventoRepository.buscarPorId(eventoId)
            .orElseThrow(() -> new EntidadNoEncontradaException("Evento", eventoId));
    }

    @Override
    protected void validarEvento(Evento evento) {
        evento.verificarDisponibleParaVenta(); // RN-02
    }

    @Override
    protected Zona obtenerZona(Long zonaId) {
        return zonaRepository.buscarPorId(zonaId)
            .orElseThrow(() -> new EntidadNoEncontradaException("Zona", zonaId));
    }

    @Override
    protected void validarCupo(Zona zona, int cantidad) {
        zona.verificarCupoDisponible(cantidad); // RN-03
    }

    @Override
    protected BigDecimal calcularPrecio(Zona zona, ComprarBoletoRequest request) {
        return zona.calcularPrecioFinal(request.metodoPago()); // RN-04
    }

    @Override
    protected EstadoPago procesarPago(ComprarBoletoRequest request, BigDecimal monto) {
        return switch (request.metodoPago()) {
            case EFECTIVO        -> EstadoPago.APROBADO;
            case TARJETA_DEBITO  -> Math.random() < 0.85 ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TARJETA_CREDITO -> Math.random() < 0.90 ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case PSE             -> Math.random() < 0.80 ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TRANSFERENCIA   -> Math.random() < 0.75 ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
        };
    }

    @Override
    protected Pago crearPago(BigDecimal monto, ComprarBoletoRequest request, EstadoPago estado) {
        return Pago.crear(monto, request.metodoPago(), estado);
    }

    @Override
    protected List<Boleto> emitirBoletos(int cantidad, BigDecimal precioUnitario,
                                          Cliente cliente, Zona zona, Pago pago) {
        List<Boleto> boletos = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            boletos.add(Boleto.emitir(precioUnitario, cliente.getId(), zona.getId(), pago)); // RN-05
        }
        return boletos;
    }

    @Override
    protected void reducirCupo(Zona zona, int cantidad) {
        zona.reducirCupo(cantidad);
        zonaRepository.guardar(zona);
    }

    @Override
    protected List<Boleto> persistirBoletos(List<Boleto> boletos) {
        List<Boleto> persistidos = new ArrayList<>(boletos.size());
        for (Boleto b : boletos) persistidos.add(boletoRepository.guardar(b));
        return persistidos;
    }

    @Override
    protected List<BoletoResponse> construirRespuesta(List<Boleto> boletos,
                                                        Cliente cliente, Evento evento, Zona zona) {
        List<BoletoResponse> respuestas = new ArrayList<>(boletos.size());
        for (Boleto b : boletos) {
            PagoResponse pagoResp = new PagoResponse(
                b.getPago().getId(), b.getPago().getMonto(), b.getPago().getMetodoPago(),
                b.getPago().getFechaPago(), b.getPago().getEstado()
            );
            respuestas.add(new BoletoResponse(
                b.getId(), b.getCodigoQR(), b.getPrecioFinal(), b.getFechaEmision(), b.getEstado(),
                cliente.getId(), cliente.getNombre() + " " + cliente.getApellido(),
                zona.getId(), zona.getNombre(),
                evento.getId(), evento.getNombre(), evento.getLugar(), evento.getFecha(),
                pagoResp
            ));
        }
        return respuestas;
    }
}