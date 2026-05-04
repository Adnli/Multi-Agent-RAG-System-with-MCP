package com.example.finnews.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt;
    private String userId;
    private String action;
    private String detail;

    public static AuditEvent of(String userId, String action, String detail) {
        AuditEvent event = new AuditEvent();
        event.createdAt = Instant.now();
        event.userId = userId;
        event.action = action;
        event.detail = detail;
        return event;
    }
}
