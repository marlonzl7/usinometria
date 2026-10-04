package com.usinometria.api.repository;

import com.usinometria.api.enums.Status;
import com.usinometria.api.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    Integer countByStatus(Status status);

    List<Job> findByStatusOrderByCreatedAtDesc(Status status);

    @Query("""
                SELECT SUM(j.volume) / SUM(j.actualTime)
                FROM Job j
                WHERE j.status = :status
            """)
    BigDecimal calculateRateByStatus(@Param("status") Status status);

    default BigDecimal calculateManufacturingTimeRate() {
        return calculateRateByStatus(Status.COMPLETED);
    }

}
