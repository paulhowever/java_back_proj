package ru.tischenko.vk.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    private String operation;
    @Column(name = "response_code", nullable = false)
    private Integer responseCode;
    @Column(name = "response_body", nullable = false)
    private String responseBody;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
}
