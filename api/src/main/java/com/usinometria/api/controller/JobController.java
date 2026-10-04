package com.usinometria.api.controller;

import com.usinometria.api.dto.*;
import com.usinometria.api.enums.Status;
import com.usinometria.api.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GetJobResponse>> find(
            @RequestParam(required = false) Status status
    ) {
        List<GetJobResponse> response = service.find(status);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetJobResponse> findById(@PathVariable Long id) {
        GetJobResponse response = service.findById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping
    public ResponseEntity<CreateJobResponse> create(
            @RequestBody @Valid CreateJobRequest request
    ) {
        CreateJobResponse response = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/rate")
    public ResponseEntity<GetManufacturingTimeRateResponse> getManufacturingTimeRate() {
        GetManufacturingTimeRateResponse response = service.getManufacturingTimeRate();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<SetCompleteJobResponse> setCompleteJob(
            @PathVariable Long id,
            @RequestBody @Valid SetCompleteJobRequest request
    ) {
        SetCompleteJobResponse response = service.setCompleteJob(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SetCancelJobResponse> setCancelJob(@PathVariable Long id) {
        SetCancelJobResponse response = service.setCancelJob(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateCompleteJobResponse> updateCompleteJob(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCompleteJobRequest request
    ) {
        UpdateCompleteJobResponse response = service.updateCompleteJob(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
