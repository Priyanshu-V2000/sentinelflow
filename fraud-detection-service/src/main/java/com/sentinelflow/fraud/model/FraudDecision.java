package com.sentinelflow.fraud.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_decisions")
public class FraudDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_event_id", nullable = false)
    private UUID paymentEventId;

    @Column(name = "fraud_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal fraudScore;

    @Column(name = "score_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal scoreThreshold;

    @Column(name = "decision", nullable = false)
    private String decision;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "model_variant", nullable = false)
    private String modelVariant = "champion";

    @Column(name = "shap_available", nullable = false)
    private Boolean shapAvailable = false;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt = Instant.now();

    // Getters and setters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID t) { this.tenantId = t; }
    public UUID getPaymentEventId() { return paymentEventId; }
    public void setPaymentEventId(UUID p) { this.paymentEventId = p; }
    public BigDecimal getFraudScore() { return fraudScore; }
    public void setFraudScore(BigDecimal f) { this.fraudScore = f; }
    public BigDecimal getScoreThreshold() { return scoreThreshold; }
    public void setScoreThreshold(BigDecimal s) { this.scoreThreshold = s; }
    public String getDecision() { return decision; }
    public void setDecision(String d) { this.decision = d; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String m) { this.modelVersion = m; }
    public String getModelVariant() { return modelVariant; }
    public void setModelVariant(String m) { this.modelVariant = m; }
    public Boolean getShapAvailable() { return shapAvailable; }
    public void setShapAvailable(Boolean s) { this.shapAvailable = s; }
    public Instant getScoredAt() { return scoredAt; }
}
