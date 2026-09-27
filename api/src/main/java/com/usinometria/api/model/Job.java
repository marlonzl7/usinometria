package com.usinometria.api.model;

import com.usinometria.api.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Column(precision = 12, scale = 2)
    private Double volume;

    @Column(precision = 7, scale = 2)
    private Double estimatedTime;

    @Column(precision = 7, scale = 2)
    private Double actualTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private Instant createdAt;

    private Instant finishedAt;

    private Instant canceledAt;

    @UpdateTimestamp
    private Instant editedAt;

}
