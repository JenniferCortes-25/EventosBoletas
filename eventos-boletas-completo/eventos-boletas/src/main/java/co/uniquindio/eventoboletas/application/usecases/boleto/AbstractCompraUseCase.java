package co.uniquindio.eventoboletas.application.usecases.boleto;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.domain.entities.*;
import co.uniquindio.eventoboletas.domain.enums.EstadoPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Patrón Template Method — clase base abstracta.
 *
 * Define el algoritmo fijo P1→P10 del CU-01 en ejecutar() (final).
 * Los pasos marcados como abstract son los puntos de extensión:
 * las subclases los implementan sin alterar el flujo general.
 */
public abstract class AbstractCompraUseCase {

    /**
     * Template: algoritmo fijo de compra. Las subclases NO pueden modificar este flujo.
     */
    public List<BoletoResponse> ejecutar(ComprarBoletoRequest request) {
        int cantidad = request.cantidad();

        // P1-P2: obtener y validar cliente (RN-01)
        Cliente cliente = obtenerCliente(request.clienteId());
        validarCliente(cliente);

        // P3-P4: obtener y validar evento (RN-02)
        Evento evento = obtenerEvento(request.eventoId());
        validarEvento(evento);

        // P5: obtener zona
        Zona zona = obtenerZona(request.zonaId());

        // P6: validar cupo (RN-03) y calcular precio (RN-04)
        validarCupo(zona, cantidad);
        BigDecimal precioUnitario = calcularPrecio(zona, request);
        BigDecimal montoTotal     = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

        // P7: procesar pago
        EstadoPago estadoPago = procesarPago(request, montoTotal);
        Pago pago = crearPago(montoTotal, request, estadoPago);

        // P8-P9: emitir boletos (RN-05) y persistir
        List<Boleto> boletos = emitirBoletos(cantidad, precioUnitario, cliente, zona, pago);
        reducirCupo(zona, cantidad);
        List<Boleto> persistidos = persistirBoletos(boletos);

        // P10: construir respuesta
        return construirRespuesta(persistidos, cliente, evento, zona);
    }

    // ── Pasos abstractos — cada subclase los implementa ─────────────────────

    protected abstract Cliente obtenerCliente(Long clienteId);
    protected abstract void   validarCliente(Cliente cliente);
    protected abstract Evento obtenerEvento(Long eventoId);
    protected abstract void   validarEvento(Evento evento);
    protected abstract Zona   obtenerZona(Long zonaId);
    protected abstract void   validarCupo(Zona zona, int cantidad);
    protected abstract BigDecimal calcularPrecio(Zona zona, ComprarBoletoRequest request);
    protected abstract EstadoPago procesarPago(ComprarBoletoRequest request, BigDecimal monto);
    protected abstract Pago crearPago(BigDecimal monto, ComprarBoletoRequest request, EstadoPago estado);
    protected abstract List<Boleto> emitirBoletos(int cantidad, BigDecimal precioUnitario,
                                                   Cliente cliente, Zona zona, Pago pago);
    protected abstract void reducirCupo(Zona zona, int cantidad);
    protected abstract List<Boleto> persistirBoletos(List<Boleto> boletos);
    protected abstract List<BoletoResponse> construirRespuesta(List<Boleto> boletos,
                                                                Cliente cliente,
                                                                Evento evento, Zona zona);
}