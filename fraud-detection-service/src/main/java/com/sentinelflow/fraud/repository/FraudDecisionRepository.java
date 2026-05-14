package com.sentinelflow.fraud.repository;

import com.sentinelflow.fraud.model.FraudDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FraudDecisionRepository extends JpaRepository<FraudDecision, UUID> {}
