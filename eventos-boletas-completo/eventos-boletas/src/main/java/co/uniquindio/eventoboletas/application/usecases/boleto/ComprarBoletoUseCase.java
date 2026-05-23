package co.uniquindio.eventoboletas.application.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.dtos.response.PagoResponse;
import co.uniquindio.eventoboletas.domain.entities.Boleto;
import co.uniquindio.eventoboletas.domain.entities.Cliente;
import co.uniquindio.eventoboletas.domain.entities.Evento;
import co.uniquindio.eventoboletas.domain.entities.Pago;
import co.uniquindio.eventoboletas.domain.entities.Zona;
import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.repositories.BoletoRepository;
import co.uniquindio.eventoboletas.domain.repositories.ClienteRepository;
import co.uniquindio.eventoboletas.domain.repositories.EventoRepository;
import co.uniquindio.eventoboletas.domain.repositories.ZonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de Uso CU-01: Comprar Boleto (transacción estrella).
 *
 * Principios SOLID aplicados:
 * - SRP: este use case orquesta SÓLO la transacción de compra de boleto.
 * - DIP: depende de abstracciones (repositorios), no de implementaciones JPA.
 * - OCP: para agregar reglas se extiende la lógica sin modificar este caso de uso.
 *
 * Patrones aplicados:
 * - Facade: orquesta múltiples repositorios y entidades de dominio en una operación.
 * - Template Method implícito: el flujo P1→P10 del CU-01 se respeta paso a paso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprarBoletoUseCase {

    private final ClienteRepository clienteRepository;
    private final EventoRepository  eventoRepository;
    private final ZonaRepository    zonaRepository;
    private final BoletoRepository  boletoRepository;

    /**
     * Ejecuta la transacción completa de compra de boleto.
     * Flujo P1→P10 del CU-01.
     *
     * Reglas validadas:
     * - RN-01: cliente activo
     * - RN-02: evento activo
     * - RN-03: cupo disponible > 0
     * - RN-04: precio calculado en servidor
     * - RN-05: boleto emitido sólo con pago aprobado
     */
    @Transactional
    public BoletoResponse ejecutar(ComprarBoletoRequest request) {
        log.info("Iniciando transacción Comprar Boleto. clienteId={}, eventoId={}, zonaId={}",
                 request.clienteId(), request.eventoId(), request.zonaId());

        // P1-P2: Buscar y validar Cliente (RN-01)
        Cliente cliente = clienteRepository.buscarPorId(request.clienteId())
            .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", request.clienteId()));
        cliente.verificarHabilitadoParaComprar(); // RN-01

        // P3-P4: Buscar y validar Evento (RN-02)
        Evento evento = eventoRepository.buscarPorId(request.eventoId())
            .orElseThrow(() -> new EntidadNoEncontradaException("Evento", request.eventoId()));
        evento.verificarDisponibleParaVenta(); // RN-02

        // P5: Buscar Zona
        Zona zona = zonaRepository.buscarPorId(request.zonaId())
            .orElseThrow(() -> new EntidadNoEncontradaException("Zona", request.zonaId()));

        // P6-P8: Calcular precio (RN-04) y validar cupo (RN-03)
        zona.verificarCupoDisponible(); // RN-03
        var precioFinal = zona.calcularPrecioFinal(); // RN-04: siempre en servidor

        // P7: Simular procesamiento del pago según método
        EstadoPago estadoPago = switch (request.metodoPago()) {
            case EFECTIVO      -> EstadoPago.APROBADO;          // siempre aprueba
            case TARJETA_DEBITO  -> Math.random() < 0.85
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TARJETA_CREDITO -> Math.random() < 0.90
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case PSE           -> Math.random() < 0.80
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TRANSFERENCIA -> Math.random() < 0.75
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
        };

        Pago pago = Pago.crear(precioFinal, request.metodoPago(), estadoPago);

        // P9: RN-05 — Boleto solo se emite si el pago fue aprobado
        Boleto boleto = Boleto.emitir(precioFinal, cliente.getId(), zona.getId(), pago);


        // Reducir cupo de la zona (invariante)
        zona.reducirCupo();
        zonaRepository.guardar(zona);

        // Persistir boleto (incluye pago en cascada)
        Boleto boletoPersistido = boletoRepository.guardar(boleto);

        log.info("Boleto emitido exitosamente. codigoQR={}, precioFinal={}",
                 boletoPersistido.getCodigoQR(), precioFinal);

        // P10: Construir y retornar comprobante
        return construirComprobante(boletoPersistido, cliente, evento, zona);
    }

    private BoletoResponse construirComprobante(Boleto boleto, Cliente cliente,
                                                Evento evento, Zona zona) {
        PagoResponse pagoResp = new PagoResponse(
            boleto.getPago().getId(),
            boleto.getPago().getMonto(),
            boleto.getPago().getMetodoPago(),
            boleto.getPago().getFechaPago(),
            boleto.getPago().getEstado()
        );

        return new BoletoResponse(
            boleto.getId(),
            boleto.getCodigoQR(),
            boleto.getPrecioFinal(),
            boleto.getFechaEmision(),
            boleto.getEstado(),
            cliente.getId(),
            cliente.getNombre() + " " + cliente.getApellido(),
            zona.getId(),
            zona.getNombre(),
            evento.getId(),
            evento.getNombre(),
            evento.getLugar(),
            evento.getFecha(),
            pagoResp
        );
    }
}
