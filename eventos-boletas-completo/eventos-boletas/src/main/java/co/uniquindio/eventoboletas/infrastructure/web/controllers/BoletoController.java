package co.uniquindio.eventoboletas.infrastructure.web.controllers;

import co.uniquindio.eventoboletas.application.dtos.request.ComprarBoletoRequest;
import co.uniquindio.eventoboletas.application.dtos.response.BoletoResponse;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.dtos.response.EventoResponse;
import co.uniquindio.eventoboletas.application.usecases.boleto.BuscarParaTransaccionUseCase;
import co.uniquindio.eventoboletas.application.usecases.boleto.ComprarBoletoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
                  "y ejecuta la compra validando las reglas de negocio RN-01 a RN-05. " +
                  "Permite comprar uno o varios boletos en una sola solicitud."
)
@RestController
@RequestMapping("/api/transaccion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BoletoController {

    private final ComprarBoletoUseCase comprarBoletoUseCase;
    private final BuscarParaTransaccionUseCase buscarUseCase;

    @Operation(
        summary = "Buscar cliente por nombre o documento",
        description = "Busca clientes activos por nombre completo o número de documento " +
                      "para identificar al comprador. (RF-01)"
    )
    @Parameter(name = "q", description = "Nombre, apellido o número de documento", example = "Juan Pérez")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes coincidentes")
    })
    @GetMapping("/clientes/buscar")
    public ResponseEntity<List<ClienteResponse>> buscarClienteParaTransaccion(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(buscarUseCase.buscarClientes(q));
    }

    @Operation(
        summary = "Listar eventos activos con zonas y precios",
        description = "Devuelve los eventos en estado ACTIVO con sus zonas, cupo disponible " +
                      "y precio base. El precio final se calcula al momento de la compra " +
                      "según el método de pago elegido. (RF-02 / RF-03)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de eventos activos con zonas")
    })
    @GetMapping("/eventos/listar-activos")
    public ResponseEntity<List<EventoResponse>> listarEventosActivos() {
        return ResponseEntity.ok(buscarUseCase.buscarEventosActivos());
    }

    @Operation(
        summary = "Comprar boleto(s)",
        description = "Ejecuta la compra completa de uno o varios boletos:\n" +
                      "- Valida cliente activo (RN-01)\n" +
                      "- Valida evento activo (RN-02)\n" +
                      "- Valida cupo disponible >= cantidad solicitada (RN-03)\n" +
                      "- Calcula precio final con recargo del método de pago (RN-04 / P6):\n" +
                      "    EFECTIVO=0%, PSE=1%, DÉBITO=2%, TRANSFERENCIA=1.5%, CRÉDITO=5%\n" +
                      "- Emite boletos solo con pago aprobado (RN-05)\n" +
                      "Retorna un comprobante por cada boleto emitido. (RF-04 / RF-05)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Boletos emitidos — devuelve lista de comprobantes con código QR",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoletoResponse.class)))
        ),
        @ApiResponse(responseCode = "400",  description = "Datos de entrada inválidos",               content = @Content),
        @ApiResponse(responseCode = "404",  description = "Cliente, evento o zona no encontrados",    content = @Content),
        @ApiResponse(responseCode = "422",  description = "Regla de negocio violada (RN-01 a RN-05)", content = @Content)
    })
    @PostMapping("/comprar-boleto")
    public ResponseEntity<List<BoletoResponse>> comprarBoleto(
            @Valid @RequestBody ComprarBoletoRequest request) {
        List<BoletoResponse> boletos = comprarBoletoUseCase.ejecutar(request);
        return ResponseEntity.ok(boletos);
    }
}