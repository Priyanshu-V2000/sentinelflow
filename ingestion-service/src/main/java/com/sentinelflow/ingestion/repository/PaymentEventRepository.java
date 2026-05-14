package com.sentinelflow.ingestion.repository;

import com.sentinelflow.ingestion.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    boolean existsByTenantIdAndTransactionId(UUID tenantId, String transactionId);
}
