package co.uniquindio.eventoboletas.infrastructure.web.controllers;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.dtos.response.EventoResponse;
import co.uniquindio.eventoboletas.application.usecases.boleto.BuscarParaTransaccionUseCase;
import co.uniquindio.eventoboletas.application.usecases.boleto.ComprarBoletoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
    name = "CU-01 · Comprar Boleto",
    description = "Transacción principal del sistema. Busca clientes y eventos activos, " +
                  "y ejecuta la compra validando las reglas de negocio RN-01 a RN-05."
)
@RestController
@RequestMapping("/api/transaccion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BoletoController {

    private final ComprarBoletoUseCase comprarBoletoUseCase;
    private final BuscarParaTransaccionUseCase buscarUseCase;

    @Operation(summary = "Buscar cliente por nombre o documento", description = "Busca clientes activos por nombre completo o número de documento para identificar al comprador. (RF-01)")
    @Parameter(name = "q", description = "Nombre, apellido o número de documento", example = "Juan Pérez")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes coincidentes")
    })
    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<ClienteResponse>> buscarClienteParaTransaccion(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(buscarUseCase.buscarClientes(q));
    }

    @Operation(summary = "Listar eventos activos con zonas y precios", description = "Devuelve los eventos en estado ACTIVO con sus zonas, cupo disponible y precio final calculado. (RF-02 / RF-03)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de eventos activos con zonas")
    })
    @GetMapping("/eventos/listar-activos")
    public ResponseEntity<List<EventoResponse>> listarEventosActivos() {
        return ResponseEntity.ok(buscarUseCase.buscarEventosActivos());
    }

    @Operation(summary = "Comprar boleto", description = "Ejecuta la compra completa: valida cliente activo (RN-01), evento activo (RN-02), cupo disponible (RN-03), límite de boletos por cliente (RN-04) y método de pago válido (RN-05). Descuenta cupo, registra pago y emite boleto con QR. (RF-04 / RF-05)")
    @ApiResponses({
        @ApiResponse(responseCode = "200",  description = "Boleto emitido — devuelve comprobante con código QR",
                     content = @Content(schema = @Schema(implementation = BoletoResponse.class))),
        @ApiResponse(responseCode = "400",  description = "Datos de entrada inválidos",          content = @Content),
        @ApiResponse(responseCode = "404",  description = "Cliente, evento o zona no encontrados", content = @Content),
        @ApiResponse(responseCode = "422",  description = "Regla de negocio violada (RN-01 a RN-05)", content = @Content)
    })
    @PostMapping("/comprar-boleto")
    public ResponseEntity<BoletoResponse> comprarBoleto(
            @Valid @RequestBody ComprarBoletoRequest request) {
        BoletoResponse boleto = comprarBoletoUseCase.ejecutar(request);
        return ResponseEntity.ok(boleto);
    }
}
