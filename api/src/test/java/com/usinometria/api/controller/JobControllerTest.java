package com.usinometria.api.controller;

import com.usinometria.api.dto.*;
import com.usinometria.api.enums.Status;
import com.usinometria.api.exception.*;
import com.usinometria.api.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes unitários da camada HTTP (sem contexto Spring, sem banco): JobController + GlobalExceptionHandler,
 * com o JobService mockado. Cobrem status codes, shape das respostas, validações e formato de erro do contrato.
 */
@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-27T14:02:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-09-28T05:35:00Z");

    @Mock
    private JobService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new JobController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Permite escrever JSON com aspas simples nos testes. */
    private static String json(String s) {
        return s.replace('\'', '"');
    }

    private static MockHttpServletRequestBuilder jsonPost(String uri, String body) {
        return post(uri).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder jsonPatch(String uri, String body, Object... vars) {
        return patch(uri, vars).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static GetJobResponse getResponse(Long id, String name, Status status) {
        return new GetJobResponse(id, name, new BigDecimal("1500.00"), new BigDecimal("15.58"),
                null, status, CREATED_AT, null, null, null);
    }

    private static SetCancelJobResponse cancelResponse(Long id) {
        return new SetCancelJobResponse(id, "Eixo pinhao", new BigDecimal("1500.00"), new BigDecimal("15.58"),
                null, Status.CANCELED, CREATED_AT, null, FINISHED_AT, null);
    }

    private static UpdateCompleteJobResponse updateResponse(Long id, String name) {
        return new UpdateCompleteJobResponse(id, name, new BigDecimal("1520.00"), new BigDecimal("15.58"),
                new BigDecimal("14.50"), Status.COMPLETED, CREATED_AT, FINISHED_AT, null, FINISHED_AT);
    }

    // ------------------------------------------------------------------
    // GET /jobs
    // ------------------------------------------------------------------

    @Test
    void getJobs_returns200WithAllJobs_whenNoFilter() throws Exception {
        when(service.find(null)).thenReturn(List.of(
                getResponse(2L, "Suporte flange", Status.IN_PROGRESS),
                getResponse(1L, "Peca inicial", Status.COMPLETED)));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Suporte flange"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }

    @Test
    void getJobs_passesStatusFilterToService() throws Exception {
        when(service.find(Status.IN_PROGRESS)).thenReturn(List.of(getResponse(2L, "Suporte flange", Status.IN_PROGRESS)));

        mockMvc.perform(get("/jobs").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));

        verify(service).find(Status.IN_PROGRESS);
    }

    @Test
    void getJobs_returnsEmptyArray_whenThereAreNoJobs() throws Exception {
        when(service.find(Status.CANCELED)).thenReturn(List.of());

        mockMvc.perform(get("/jobs").param("status", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getJobs_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // GET /jobs/{id}
    // ------------------------------------------------------------------

    @Test
    void getJobById_returns200WithJob() throws Exception {
        when(service.findById(25L)).thenReturn(getResponse(25L, "Eixo pinhao", Status.IN_PROGRESS));

        mockMvc.perform(get("/jobs/{id}", 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("Eixo pinhao"))
                .andExpect(jsonPath("$.volume").value(1500.00))
                .andExpect(jsonPath("$.estimatedTime").value(15.58))
                .andExpect(jsonPath("$.actualTime").value(nullValue()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getJobById_returns404_whenJobDoesNotExist() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException());

        mockMvc.perform(get("/jobs/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.details").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getJobById_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/jobs/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // GET /jobs/rate
    // ------------------------------------------------------------------

    @Test
    void getRate_returns200WithRateAndQuantity() throws Exception {
        when(service.getManufacturingTimeRate())
                .thenReturn(new GetManufacturingTimeRateResponse(new BigDecimal("96.25000"), 18));

        mockMvc.perform(get("/jobs/rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(96.25))
                .andExpect(jsonPath("$.quantity").value(18));
    }

    @Test
    void getRate_returnsNullRateAndZeroQuantity_whenThereIsNoHistory() throws Exception {
        when(service.getManufacturingTimeRate())
                .thenReturn(new GetManufacturingTimeRateResponse(null, 0));

        mockMvc.perform(get("/jobs/rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(nullValue()))
                .andExpect(jsonPath("$.quantity").value(0));
    }

    // ------------------------------------------------------------------
    // POST /jobs
    // ------------------------------------------------------------------

    @Test
    void createJob_returns201WithEstimate() throws Exception {
        when(service.create(any(CreateJobRequest.class))).thenReturn(new CreateJobResponse(
                25L, "Eixo pinhao", new BigDecimal("1500.00"), new BigDecimal("15.58"), null,
                Status.IN_PROGRESS, new BigDecimal("96.25000"), CREATED_AT, null, null, null));

        mockMvc.perform(jsonPost("/jobs", json("{'name':'Eixo pinhao','volume':1500.00}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("Eixo pinhao"))
                .andExpect(jsonPath("$.volume").value(1500.00))
                .andExpect(jsonPath("$.estimatedTime").value(15.58))
                .andExpect(jsonPath("$.rateUsed").value(96.25))
                .andExpect(jsonPath("$.actualTime").value(nullValue()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        ArgumentCaptor<CreateJobRequest> captor = ArgumentCaptor.forClass(CreateJobRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Eixo pinhao");
        assertThat(captor.getValue().volume()).isEqualByComparingTo("1500.00");
    }

    static Stream<Arguments> invalidCreateBodies() {
        return Stream.of(
                Arguments.of("name ausente", json("{'volume':1500.00}"), "name"),
                Arguments.of("name em branco", json("{'name':'   ','volume':1500.00}"), "name"),
                Arguments.of("name com mais de 150 caracteres",
                        json("{'name':'" + "a".repeat(151) + "','volume':1500.00}"), "name"),
                Arguments.of("volume ausente", json("{'name':'Eixo'}"), "volume"),
                Arguments.of("volume zero", json("{'name':'Eixo','volume':0}"), "volume"),
                Arguments.of("volume negativo", json("{'name':'Eixo','volume':-5}"), "volume"),
                Arguments.of("volume com 3 casas decimais", json("{'name':'Eixo','volume':1500.123}"), "volume"),
                Arguments.of("volume com 11 digitos inteiros", json("{'name':'Eixo','volume':12345678901}"), "volume")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateBodies")
    void createJob_returns400_whenBodyIsInvalid(String caso, String body, String field) throws Exception {
        mockMvc.perform(jsonPost("/jobs", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasItem(startsWith(field + ":"))));

        verifyNoInteractions(service);
    }

    @Test
    void createJob_reportsOneDetailPerInvalidField() throws Exception {
        mockMvc.perform(jsonPost("/jobs", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasSize(2)))
                .andExpect(jsonPath("$.details", hasItem(startsWith("name:"))))
                .andExpect(jsonPath("$.details", hasItem(startsWith("volume:"))));
    }

    @Test
    void createJob_returns400_whenJsonIsMalformed() throws Exception {
        mockMvc.perform(jsonPost("/jobs", "{name: nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void createJob_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void createJob_returns415_whenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/jobs").contentType(MediaType.TEXT_PLAIN).content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(service);
    }

    @Test
    void createJob_returns422_whenThereIsNoHistory() throws Exception {
        when(service.create(any(CreateJobRequest.class))).thenThrow(new NoHistoryAvailableException());

        mockMvc.perform(jsonPost("/jobs", json("{'name':'Eixo','volume':1500.00}")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("NO_HISTORY_AVAILABLE"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    @Test
    void createJob_returns422_whenEstimateIsOutOfRange() throws Exception {
        when(service.create(any(CreateJobRequest.class))).thenThrow(new EstimateOutOfRangeException());

        mockMvc.perform(jsonPost("/jobs", json("{'name':'Eixo','volume':9999999999.99}")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("ESTIMATE_OUT_OF_RANGE"));
    }

    // ------------------------------------------------------------------
    // PATCH /jobs/{id}/complete
    // ------------------------------------------------------------------

    @Test
    void completeJob_returns200WithDifference() throws Exception {
        when(service.setCompleteJob(eqId(25L), any(SetCompleteJobRequest.class))).thenReturn(new SetCompleteJobResponse(
                25L, "Eixo pinhao", new BigDecimal("1500.00"), new BigDecimal("15.58"), new BigDecimal("14.70"),
                Status.COMPLETED, new BigDecimal("-0.88"), CREATED_AT, FINISHED_AT, null, null));

        mockMvc.perform(jsonPatch("/jobs/{id}/complete", json("{'actualTime':14.70}"), 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualTime").value(14.70))
                .andExpect(jsonPath("$.difference").value(-0.88))
                .andExpect(jsonPath("$.finishedAt").exists())
                .andExpect(jsonPath("$.canceledAt").value(nullValue()));

        ArgumentCaptor<SetCompleteJobRequest> captor = ArgumentCaptor.forClass(SetCompleteJobRequest.class);
        verify(service).setCompleteJob(eqId(25L), captor.capture());
        assertThat(captor.getValue().actualTime()).isEqualByComparingTo("14.70");
    }

    static Stream<Arguments> invalidCompleteBodies() {
        return Stream.of(
                Arguments.of("actualTime ausente", "{}"),
                Arguments.of("actualTime zero", json("{'actualTime':0}")),
                Arguments.of("actualTime negativo", json("{'actualTime':-1}")),
                Arguments.of("actualTime com 6 digitos inteiros", json("{'actualTime':123456}")),
                Arguments.of("actualTime com 3 casas decimais", json("{'actualTime':14.705}"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCompleteBodies")
    void completeJob_returns400_whenBodyIsInvalid(String caso, String body) throws Exception {
        mockMvc.perform(jsonPatch("/jobs/{id}/complete", body, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasItem(startsWith("actualTime:"))));

        verifyNoInteractions(service);
    }

    @Test
    void completeJob_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(patch("/jobs/{id}/complete", 25).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void completeJob_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(jsonPatch("/jobs/{id}/complete", json("{'actualTime':14.70}"), "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));

        verifyNoInteractions(service);
    }

    @Test
    void completeJob_returns404_whenJobDoesNotExist() throws Exception {
        when(service.setCompleteJob(eqId(99L), any(SetCompleteJobRequest.class)))
                .thenThrow(new ResourceNotFoundException());

        mockMvc.perform(jsonPatch("/jobs/{id}/complete", json("{'actualTime':14.70}"), 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void completeJob_returns409_whenJobIsNotInProgress() throws Exception {
        when(service.setCompleteJob(eqId(25L), any(SetCompleteJobRequest.class)))
                .thenThrow(new UnexpectedStatusJobException("IN_PROGRESS"));

        mockMvc.perform(jsonPatch("/jobs/{id}/complete", json("{'actualTime':14.70}"), 25))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("UNEXPECTED_STATUS_JOB"))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    // ------------------------------------------------------------------
    // PATCH /jobs/{id}/cancel
    // ------------------------------------------------------------------

    @Test
    void cancelJob_returns200WithCanceledJob() throws Exception {
        when(service.setCancelJob(25L)).thenReturn(cancelResponse(25L));

        mockMvc.perform(patch("/jobs/{id}/cancel", 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").exists())
                .andExpect(jsonPath("$.finishedAt").value(nullValue()));
    }

    @Test
    void cancelJob_returns404_whenJobDoesNotExist() throws Exception {
        when(service.setCancelJob(99L)).thenThrow(new ResourceNotFoundException());

        mockMvc.perform(patch("/jobs/{id}/cancel", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void cancelJob_returns409_whenJobIsNotInProgress() throws Exception {
        when(service.setCancelJob(25L)).thenThrow(new UnexpectedStatusJobException("IN_PROGRESS"));

        mockMvc.perform(patch("/jobs/{id}/cancel", 25))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UNEXPECTED_STATUS_JOB"));
    }

    @Test
    void cancelJob_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(patch("/jobs/{id}/cancel", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // PATCH /jobs/{id}  (edição de trabalho concluído)
    // ------------------------------------------------------------------

    @Test
    void updateJob_returns200_forPartialUpdate() throws Exception {
        when(service.updateCompleteJob(eqId(25L), any(UpdateCompleteJobRequest.class)))
                .thenReturn(updateResponse(25L, "Novo nome"));

        mockMvc.perform(jsonPatch("/jobs/{id}", json("{'name':'Novo nome'}"), 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo nome"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.editedAt").exists());

        ArgumentCaptor<UpdateCompleteJobRequest> captor = ArgumentCaptor.forClass(UpdateCompleteJobRequest.class);
        verify(service).updateCompleteJob(eqId(25L), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Novo nome");
        assertThat(captor.getValue().volume()).isNull();
        assertThat(captor.getValue().actualTime()).isNull();
    }

    @Test
    void updateJob_returns200_whenAllFieldsAreSent() throws Exception {
        when(service.updateCompleteJob(eqId(25L), any(UpdateCompleteJobRequest.class)))
                .thenReturn(updateResponse(25L, "Eixo corrigido"));

        mockMvc.perform(jsonPatch("/jobs/{id}",
                        json("{'name':'Eixo corrigido','volume':1520.00,'actualTime':14.50}"), 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume").value(1520.00))
                .andExpect(jsonPath("$.actualTime").value(14.50));

        ArgumentCaptor<UpdateCompleteJobRequest> captor = ArgumentCaptor.forClass(UpdateCompleteJobRequest.class);
        verify(service).updateCompleteJob(eqId(25L), captor.capture());
        assertThat(captor.getValue().volume()).isEqualByComparingTo("1520.00");
        assertThat(captor.getValue().actualTime()).isEqualByComparingTo("14.50");
    }

    static Stream<Arguments> invalidUpdateBodies() {
        return Stream.of(
                Arguments.of("name em branco", json("{'name':'   '}"), "name"),
                Arguments.of("name com mais de 150 caracteres", json("{'name':'" + "a".repeat(151) + "'}"), "name"),
                Arguments.of("volume zero", json("{'volume':0}"), "volume"),
                Arguments.of("volume com 3 casas decimais", json("{'volume':10.123}"), "volume"),
                Arguments.of("actualTime negativo", json("{'actualTime':-1}"), "actualTime"),
                Arguments.of("actualTime com 6 digitos inteiros", json("{'actualTime':123456}"), "actualTime")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdateBodies")
    void updateJob_returns400_whenBodyIsInvalid(String caso, String body, String field) throws Exception {
        mockMvc.perform(jsonPatch("/jobs/{id}", body, 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasItem(startsWith(field + ":"))));

        verifyNoInteractions(service);
    }

    @Test
    void updateJob_returns400_whenNoFieldIsSent() throws Exception {
        when(service.updateCompleteJob(eqId(25L), any(UpdateCompleteJobRequest.class)))
                .thenThrow(new EmptyUpdateException());

        mockMvc.perform(jsonPatch("/jobs/{id}", "{}", 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("EMPTY_UPDATE_REQUEST"));
    }

    @Test
    void updateJob_returns400_whenJsonIsMalformed() throws Exception {
        mockMvc.perform(jsonPatch("/jobs/{id}", "{name:", 25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verifyNoInteractions(service);
    }

    @Test
    void updateJob_returns404_whenJobDoesNotExist() throws Exception {
        when(service.updateCompleteJob(eqId(99L), any(UpdateCompleteJobRequest.class)))
                .thenThrow(new ResourceNotFoundException());

        mockMvc.perform(jsonPatch("/jobs/{id}", json("{'name':'Novo nome'}"), 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateJob_returns409_whenJobIsNotCompleted() throws Exception {
        when(service.updateCompleteJob(eqId(25L), any(UpdateCompleteJobRequest.class)))
                .thenThrow(new UnexpectedStatusJobException("COMPLETED"));

        mockMvc.perform(jsonPatch("/jobs/{id}", json("{'name':'Novo nome'}"), 25))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UNEXPECTED_STATUS_JOB"));
    }

    @Test
    void updateJob_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(jsonPatch("/jobs/{id}", json("{'name':'Novo nome'}"), "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));

        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // Erros transversais (GlobalExceptionHandler)
    // ------------------------------------------------------------------

    @Test
    void returns405_whenHttpMethodIsNotAllowed() throws Exception {
        mockMvc.perform(delete("/jobs/{id}", 1))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));

        verifyNoInteractions(service);
    }

    @Test
    void returns500WithGenericMessage_andDoesNotLeakInternalDetails_whenUnexpectedErrorOccurs() throws Exception {
        when(service.find(null)).thenThrow(new RuntimeException("boom: connection to db refused"));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message", not(containsString("boom"))))
                .andExpect(jsonPath("$.message", not(containsString("db"))))
                .andExpect(jsonPath("$.details").value(nullValue()));
    }

    // ------------------------------------------------------------------

    /** Matcher de Long para o id (evita import estático repetido de eq). */
    private static Long eqId(long id) {
        return org.mockito.ArgumentMatchers.eq(id);
    }

}