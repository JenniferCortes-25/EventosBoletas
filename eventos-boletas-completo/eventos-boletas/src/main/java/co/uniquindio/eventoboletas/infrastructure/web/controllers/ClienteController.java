package co.uniquindio.eventoboletas.infrastructure.web.controllers;

import co.uniquindio.eventoboletas.application.dtos.request.ClienteRequest;
import co.uniquindio.eventoboletas.application.dtos.response.ClienteResponse;
import co.uniquindio.eventoboletas.application.dtos.response.PagedResponse;
import co.uniquindio.eventoboletas.application.usecases.cliente.GestionarClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "CU-02 · Gestionar Clientes",
    description = "CRUD completo de clientes. Valida email único (RN-07) en crear/editar " +
                  "y que no tenga boletos pagados (RN-06) al eliminar."
)
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClienteController {

    private final GestionarClienteUseCase gestionarClienteUseCase;

    @Operation(summary = "Listar clientes paginados", description = "Devuelve todos los clientes con paginación. (RF-06)")
    @Parameter(name = "pagina", description = "Número de página (base 0)", example = "0")
    @Parameter(name = "tamano", description = "Registros por página", example = "10")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de clientes devuelta correctamente")
    })
    @GetMapping("/listar")
    public ResponseEntity<PagedResponse<ClienteResponse>> listarClientes(
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return ResponseEntity.ok(gestionarClienteUseCase.listar(pagina, tamano));
    }

    @Operation(summary = "Buscar cliente por id", description = "Devuelve los datos de un cliente a partir de su id.")
    @Parameter(name = "id", description = "Id del cliente", example = "1", required = true)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                     content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @GetMapping("/{id}/buscar-por-id")
    public ResponseEntity<ClienteResponse> buscarClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(gestionarClienteUseCase.buscarPorId(id));
    }

    @Operation(summary = "Crear cliente", description = "Registra un cliente nuevo. Valida que el email no esté en uso (RN-07). (RF-07)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente creado correctamente",
                     content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",  content = @Content),
        @ApiResponse(responseCode = "422", description = "Email ya registrado (RN-07)", content = @Content)
    })
    @PostMapping("/crear")
    public ResponseEntity<ClienteResponse> crearCliente(
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(gestionarClienteUseCase.crear(request));
    }

    @Operation(summary = "Editar cliente", description = "Actualiza los datos de un cliente. Valida email único en otro registro (RN-07). (RF-08)")
    @Parameter(name = "id", description = "Id del cliente a editar", example = "1", required = true)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente actualizado correctamente",
                     content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",  content = @Content),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",        content = @Content),
        @ApiResponse(responseCode = "422", description = "Email ya registrado (RN-07)", content = @Content)
    })
    @PutMapping("/{id}/editar")
    public ResponseEntity<ClienteResponse> editarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(gestionarClienteUseCase.editar(id, request));
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente. Falla si tiene boletos pagados (RN-06). Devuelve 204 si fue exitoso. (RF-09)")
    @Parameter(name = "id", description = "Id del cliente a eliminar", example = "1", required = true)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cliente eliminado correctamente", content = @Content),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",            content = @Content),
        @ApiResponse(responseCode = "422", description = "Tiene boletos pagados (RN-06)",    content = @Content)
    })
    @DeleteMapping("/{id}/eliminar")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        gestionarClienteUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
