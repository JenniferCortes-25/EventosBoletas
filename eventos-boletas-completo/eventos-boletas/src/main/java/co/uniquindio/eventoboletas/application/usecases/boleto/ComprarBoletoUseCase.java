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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
 *
 * Cambios respecto a la versión anterior:
 * - P6 cumplido: el precio final incluye el recargo propio del método de pago
 *   (EFECTIVO = 0%, PSE = 1%, DÉBITO = 2%, TRANSFERENCIA = 1,5%, CRÉDITO = 5%).
 * - Se soporta compra de N boletos en una sola transacción (campo {@code cantidad}).
 *   El cupo se valida y reduce en bloque; el pago cubre el monto total (precioUnitario × N).
 *   Se retorna {@code List<BoletoResponse>} con un comprobante por boleto emitido.
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
     * Ejecuta la transacción completa de compra de uno o varios boletos.
     * Flujo P1→P10 del CU-01.
     *
     * Reglas validadas:
     * - RN-01: cliente activo
     * - RN-02: evento activo
     * - RN-03: cupo disponible >= cantidad solicitada
     * - RN-04: precio calculado en servidor (precioBase × (1 + recargo_zona + recargo_metodoPago))
     * - RN-05: boleto emitido sólo con pago aprobado
     *
     * @param request DTO con clienteId, eventoId, zonaId, metodoPago y cantidad.
     * @return Lista de comprobantes, uno por boleto emitido.
     */
    @Transactional
    public List<BoletoResponse> ejecutar(ComprarBoletoRequest request) {

        int cantidad = request.cantidad();

        log.info("Iniciando transacción Comprar Boleto. clienteId={}, eventoId={}, zonaId={}, cantidad={}",
                 request.clienteId(), request.eventoId(), request.zonaId(), cantidad);

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

        // P6: Verificar cupo para la cantidad solicitada (RN-03)
        //     y calcular precio unitario con recargo del método de pago (RN-04)
        zona.verificarCupoDisponible(cantidad); // RN-03
        BigDecimal precioUnitario = zona.calcularPrecioFinal(request.metodoPago()); // RN-04
        BigDecimal montoTotal     = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

        log.info("Precio unitario calculado={} (recargo método={}%), montoTotal={}",
                 precioUnitario,
                 request.metodoPago().getRecargoPorcentaje().multiply(new BigDecimal("100")).toPlainString(),
                 montoTotal);

        // P7: Simular procesamiento del pago según método
        //     Un solo pago cubre la totalidad de los boletos solicitados.
        EstadoPago estadoPago = switch (request.metodoPago()) {
            case EFECTIVO       -> EstadoPago.APROBADO;           // siempre aprueba
            case TARJETA_DEBITO  -> Math.random() < 0.85
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TARJETA_CREDITO -> Math.random() < 0.90
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case PSE            -> Math.random() < 0.80
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
            case TRANSFERENCIA  -> Math.random() < 0.75
                    ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
        };

        // El monto del pago es el total (precioUnitario × cantidad)
        Pago pago = Pago.crear(montoTotal, request.metodoPago(), estadoPago);

        // P8-P9: RN-05 — Cada boleto solo se emite si el pago fue aprobado
        //         precioFinal del boleto es el precio UNITARIO (no el total)
        List<Boleto> boletos = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            boletos.add(Boleto.emitir(precioUnitario, cliente.getId(), zona.getId(), pago));
        }

        // Reducir cupo en bloque (invariante de dominio)
        zona.reducirCupo(cantidad);
        zonaRepository.guardar(zona);

        // Persistir todos los boletos (incluye pago en cascada por el primero)
        List<Boleto> persistidos = new ArrayList<>(cantidad);
        for (Boleto boleto : boletos) {
            persistidos.add(boletoRepository.guardar(boleto));
        }

        log.info("Boletos emitidos exitosamente. cantidad={}, precioUnitario={}, montoTotal={}",
                 cantidad, precioUnitario, montoTotal);

        // P10: Construir y retornar comprobantes (uno por boleto)
        List<BoletoResponse> comprobantes = new ArrayList<>(cantidad);
        for (Boleto boletoPersistido : persistidos) {
            comprobantes.add(construirComprobante(boletoPersistido, cliente, evento, zona));
        }
        return comprobantes;
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

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