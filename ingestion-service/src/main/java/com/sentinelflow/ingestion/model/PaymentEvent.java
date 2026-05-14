package com.sentinelflow.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_events",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id","transaction_id"}))
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "transaction_id", nullable = false)
    @NotBlank
    private String transactionId;

    @Column(name = "card_hash", nullable = false)
    @NotBlank
    private String cardHash;

    @Column(nullable = false, precision = 15, scale = 2)
    @DecimalMin("0.01")
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "CHAR(3)")
    @NotBlank @Size(min=3, max=3)
    private String currency;

    @Column(name = "merchant_id", nullable = false)
    @NotBlank
    private String merchantId;

    @Column(name = "merchant_cat")
    private String merchantCat;

    @Column(name = "country_code", columnDefinition = "CHAR(2)")
    private String countryCode;

    private String city;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String t) { this.transactionId = t; }
    public String getCardHash() { return cardHash; }
    public void setCardHash(String c) { this.cardHash = c; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public String getCurrency() { return currency; }
    public void setCurrency(String c) { this.currency = c; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String m) { this.merchantId = m; }
    public String getMerchantCat() { return merchantCat; }
    public void setMerchantCat(String m) { this.merchantCat = m; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String c) { this.countryCode = c; }
    public String getCity() { return city; }
    public void setCity(String c) { this.city = c; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant e) { this.eventTime = e; }
    public Instant getCreatedAt() { return createdAt; }
}
