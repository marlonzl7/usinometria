package com.usinometria.api.controller;

import com.usinometria.api.common.ErrorResponse;
import com.usinometria.api.dto.*;
import com.usinometria.api.enums.Status;
import com.usinometria.api.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@Tag(
        name = "Jobs",
        description = "Trabalhos de usinagem: criação com estimativa, conclusão, cancelamento, edição e taxa empírica. "
                + "Volume em mm³; tempos em horas (decimal)."
)
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Lista trabalhos",
            description = "Retorna os trabalhos do mais recente para o mais antigo. Use `status` para filtrar."
    )
    @ApiResponse(responseCode = "200", description = "Lista de trabalhos (vazia se não houver nenhum)",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = GetJobResponse.class))))
    @ApiResponse(responseCode = "400", description = "`status` inválido (INVALID_PARAMETER)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<GetJobResponse>> find(
            @Parameter(description = "Filtra por status do trabalho", example = "IN_PROGRESS")
            @RequestParam(required = false) Status status
    ) {
        List<GetJobResponse> response = service.find(status);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um trabalho pelo ID")
    @ApiResponse(responseCode = "200", description = "Trabalho encontrado")
    @ApiResponse(responseCode = "400", description = "ID não numérico (INVALID_PARAMETER)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Trabalho não existe (RESOURCE_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GetJobResponse> findById(
            @Parameter(description = "ID do trabalho", example = "25") @PathVariable Long id
    ) {
        GetJobResponse response = service.findById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping
    @Operation(
            summary = "Cria um trabalho e calcula a estimativa",
            description = "Calcula `estimatedTime = volume / rate`, onde `rate` é a taxa global "
                    + "(ΣVolume / ΣTempoReal) dos trabalhos COMPLETED. O trabalho nasce como IN_PROGRESS."
    )
    @ApiResponse(responseCode = "201", description = "Trabalho criado com a estimativa e a taxa utilizada")
    @ApiResponse(responseCode = "400",
            description = "Corpo inválido (VALIDATION_ERROR, com `details` por campo) ou malformado (MALFORMED_REQUEST)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type diferente de application/json (UNSUPPORTED_MEDIA_TYPE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422",
            description = "Sem histórico para calcular a taxa (NO_HISTORY_AVAILABLE) "
                    + "ou estimativa acima do limite suportado (ESTIMATE_OUT_OF_RANGE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CreateJobResponse> create(
            @RequestBody @Valid CreateJobRequest request
    ) {
        CreateJobResponse response = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/rate")
    @Operation(
            summary = "Taxa empírica atual",
            description = "Taxa global em mm³/h e quantidade de trabalhos COMPLETED que a sustentam. "
                    + "Sem histórico, retorna `rate = null` e `quantity = 0`."
    )
    @ApiResponse(responseCode = "200", description = "Taxa atual")
    public ResponseEntity<GetManufacturingTimeRateResponse> getManufacturingTimeRate() {
        GetManufacturingTimeRateResponse response = service.getManufacturingTimeRate();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Conclui um trabalho informando o tempo real",
            description = "Só vale para trabalhos IN_PROGRESS. O trabalho passa a COMPLETED e entra no cálculo da taxa. "
                    + "`difference = actualTime - estimatedTime` (positivo = demorou mais que o estimado)."
    )
    @ApiResponse(responseCode = "200", description = "Trabalho concluído")
    @ApiResponse(responseCode = "400", description = "ID ou corpo inválido (INVALID_PARAMETER, VALIDATION_ERROR, MALFORMED_REQUEST)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Trabalho não existe (RESOURCE_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trabalho não está IN_PROGRESS (UNEXPECTED_STATUS_JOB)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SetCompleteJobResponse> setCompleteJob(
            @Parameter(description = "ID do trabalho", example = "25") @PathVariable Long id,
            @RequestBody @Valid SetCompleteJobRequest request
    ) {
        SetCompleteJobResponse response = service.setCompleteJob(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancela um trabalho em fabricação",
            description = "Só vale para trabalhos IN_PROGRESS. Trabalhos CANCELED nunca entram no cálculo da taxa."
    )
    @ApiResponse(responseCode = "200", description = "Trabalho cancelado")
    @ApiResponse(responseCode = "400", description = "ID não numérico (INVALID_PARAMETER)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Trabalho não existe (RESOURCE_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trabalho não está IN_PROGRESS (UNEXPECTED_STATUS_JOB)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SetCancelJobResponse> setCancelJob(
            @Parameter(description = "ID do trabalho", example = "25") @PathVariable Long id
    ) {
        SetCancelJobResponse response = service.setCancelJob(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Edita um trabalho concluído (atualização parcial)",
            description = "Só vale para trabalhos COMPLETED. Envie ao menos um entre `name`, `volume` e `actualTime`; "
                    + "campos omitidos permanecem como estão. Preenche `editedAt`. "
                    + "Alterar `volume` ou `actualTime` muda a taxa global."
    )
    @ApiResponse(responseCode = "200", description = "Trabalho atualizado")
    @ApiResponse(responseCode = "400",
            description = "ID ou corpo inválido, ou nenhum campo enviado (EMPTY_UPDATE_REQUEST)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Trabalho não existe (RESOURCE_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trabalho não está COMPLETED (UNEXPECTED_STATUS_JOB)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UpdateCompleteJobResponse> updateCompleteJob(
            @Parameter(description = "ID do trabalho", example = "25") @PathVariable Long id,
            @RequestBody @Valid UpdateCompleteJobRequest request
    ) {
        UpdateCompleteJobResponse response = service.updateCompleteJob(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}