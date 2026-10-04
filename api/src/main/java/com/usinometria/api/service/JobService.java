package com.usinometria.api.service;

import com.usinometria.api.dto.*;
import com.usinometria.api.enums.Status;
import com.usinometria.api.exception.EmptyUpdateException;
import com.usinometria.api.exception.EstimateOutOfRangeException;
import com.usinometria.api.exception.NoHistoryAvailableException;
import com.usinometria.api.exception.ResourceNotFoundException;
import com.usinometria.api.exception.UnexpectedStatusJobException;
import com.usinometria.api.mapper.JobMapper;
import com.usinometria.api.model.Job;
import com.usinometria.api.repository.JobRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class JobService {

    private static final BigDecimal MAX_ESTIMATED_TIME = new BigDecimal("99999.99");
    private static final int RATE_DISPLAY_SCALE = 5;

    private final JobRepository repository;
    private final JobMapper mapper;

    public JobService(JobRepository repository, JobMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GetJobResponse> find(Status status) {
        List<Job> jobs = status != null
                ? repository.findByStatusOrderByCreatedAtDesc(status)
                : repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return jobs.stream()
                .map(mapper::toGetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GetJobResponse findById(Long id) {
        return mapper.toGetResponse(findJobOrThrow(id));
    }

    @Transactional
    public CreateJobResponse create(CreateJobRequest request) {
        BigDecimal rate = repository.calculateManufacturingTimeRate();

        if (rate == null || rate.signum() <= 0) {
            throw new NoHistoryAvailableException();
        }

        BigDecimal volume = request.volume();
        BigDecimal estimatedTime = volume.divide(rate, 2, RoundingMode.HALF_UP);

        if (estimatedTime.compareTo(MAX_ESTIMATED_TIME) > 0) {
            throw new EstimateOutOfRangeException();
        }

        Job job = new Job();
        job.setName(request.name().trim());
        job.setVolume(volume);
        job.setEstimatedTime(estimatedTime);
        job.setStatus(Status.IN_PROGRESS);

        repository.save(job);

        return mapper.toCreateResponse(job, roundRate(rate));
    }

    @Transactional(readOnly = true)
    public GetManufacturingTimeRateResponse getManufacturingTimeRate() {
        int quantity = repository.countByStatus(Status.COMPLETED);

        if (quantity == 0) {
            return new GetManufacturingTimeRateResponse(null, 0);
        }

        BigDecimal rate = repository.calculateManufacturingTimeRate();

        return mapper.toGetManufacturingTimeRateResponse(roundRate(rate), quantity);
    }

    @Transactional
    public SetCompleteJobResponse setCompleteJob(Long id, SetCompleteJobRequest request) {
        Job job = findJobOrThrow(id);

        if (job.getStatus() != Status.IN_PROGRESS) {
            throw new UnexpectedStatusJobException("IN_PROGRESS");
        }

        job.setStatus(Status.COMPLETED);
        job.setActualTime(request.actualTime());
        job.setFinishedAt(Instant.now());

        BigDecimal difference = request.actualTime().subtract(job.getEstimatedTime());

        return mapper.toSetCompleteResponse(job, difference);
    }

    @Transactional
    public SetCancelJobResponse setCancelJob(Long id) {
        Job job = findJobOrThrow(id);

        if (job.getStatus() != Status.IN_PROGRESS) {
            throw new UnexpectedStatusJobException("IN_PROGRESS");
        }

        job.setStatus(Status.CANCELED);
        job.setCanceledAt(Instant.now());

        return mapper.toSetCancelResponse(job);
    }

    @Transactional
    public UpdateCompleteJobResponse updateCompleteJob(Long id, UpdateCompleteJobRequest request) {
        Job job = findJobOrThrow(id);

        if (job.getStatus() != Status.COMPLETED) {
            throw new UnexpectedStatusJobException("COMPLETED");
        }

        if (request.name() == null && request.volume() == null && request.actualTime() == null) {
            throw new EmptyUpdateException();
        }

        if (request.name() != null) {
            job.setName(request.name().trim());
        }
        if (request.volume() != null) {
            job.setVolume(request.volume());
        }
        if (request.actualTime() != null) {
            job.setActualTime(request.actualTime());
        }

        job.setEditedAt(Instant.now());

        return mapper.toUpdateCompleteResponse(job);
    }

    private Job findJobOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private BigDecimal roundRate(BigDecimal rate) {
        return rate.setScale(RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

}
