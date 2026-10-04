package com.usinometria.api.mapper;

import com.usinometria.api.dto.*;
import com.usinometria.api.model.Job;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class JobMapper {

    public GetJobResponse toGetResponse(Job job) {
        return new GetJobResponse(
                job.getId(),
                job.getName(),
                job.getVolume(),
                job.getEstimatedTime(),
                job.getActualTime(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getCanceledAt(),
                job.getEditedAt()
        );
    }

    public CreateJobResponse toCreateResponse(Job job, BigDecimal rateUsed) {
        return new CreateJobResponse(
                job.getId(),
                job.getName(),
                job.getVolume(),
                job.getEstimatedTime(),
                job.getActualTime(),
                job.getStatus(),
                rateUsed,
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getCanceledAt(),
                job.getEditedAt()
        );
    }

    public GetManufacturingTimeRateResponse toGetManufacturingTimeRateResponse(BigDecimal rate, Integer quantity) {
        return new GetManufacturingTimeRateResponse(rate, quantity);
    }

    public SetCompleteJobResponse toSetCompleteResponse(Job job, BigDecimal difference) {
        return new SetCompleteJobResponse(
                job.getId(),
                job.getName(),
                job.getVolume(),
                job.getEstimatedTime(),
                job.getActualTime(),
                job.getStatus(),
                difference,
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getCanceledAt(),
                job.getEditedAt()
        );
    }

    public SetCancelJobResponse toSetCancelResponse(Job job) {
        return new SetCancelJobResponse(
                job.getId(),
                job.getName(),
                job.getVolume(),
                job.getEstimatedTime(),
                job.getActualTime(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getCanceledAt(),
                job.getEditedAt()
        );
    }

    public UpdateCompleteJobResponse toUpdateCompleteResponse(Job job) {
        return new UpdateCompleteJobResponse(
                job.getId(),
                job.getName(),
                job.getVolume(),
                job.getEstimatedTime(),
                job.getActualTime(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getCanceledAt(),
                job.getEditedAt()
        );
    }

}
