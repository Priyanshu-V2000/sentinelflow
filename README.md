# 🛡️ SentinelFlow — AI-Powered Payment Fraud Detection Platform

[![CI](https://github.com/Priyanshu-V2000/sentinelflow/actions/workflows/ci.yml/badge.svg)](https://github.com/Priyanshu-V2000/sentinelflow/actions/workflows/ci.yml)

A production-grade, real-time payment fraud detection platform built with microservices architecture, Apache Kafka, and AI-powered fraud scoring.

---

## 🏗️ Architecture
┌─────────────────────────────────────────────────────────────┐
│                     React Dashboard (5173)                   │
└─────────────────────┬───────────────────────────────────────┘
│ HTTP
┌─────────────────────▼───────────────────────────────────────┐
│           API Gateway (8080) — JWT + Rate Limiting           │
└──────┬──────────────┬──────────────┬────────────────────────┘
│              │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌───▼────────────┐
│  Ingestion  │ │ Analytics  │ │   AI Insight   │
│   (8081)    │ │   (8082)   │ │    (8084)      │
└──────┬──────┘ └─────▲──────┘ └───▲────────────┘
│ Kafka         │ Kafka      │ Kafka
│         ┌─────┴──────┐    │
└────────►│   Fraud    │────┘
│ Detection  │
│   (8083)   │
└─────┬──────┘
│
┌──────────────▼──────────────┐
│   PostgreSQL + pgvector     │
│   Redis  │  Kafka  │ Jaeger │
└─────────────────────────────┘
## 🚀 Services

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | JWT auth, rate limiting, routing |
| Ingestion Service | 8081 | Payment event ingestion + Kafka producer |
| Analytics Service | 8082 | Real-time fraud metrics aggregation |
| Fraud Detection | 8083 | ML fraud scoring pipeline |
| AI Insight | 8084 | RAG-based fraud explanation (SSE streaming) |
| React Dashboard | 5173 | Real-time monitoring UI |

## 🐳 Infrastructure

| Component | Version | Purpose |
|---|---|---|
| PostgreSQL + pgvector | 0.5.1 | Payment events + vector embeddings |
| Apache Kafka | 3.7 KRaft | Event streaming between services |
| Redis | 7.2 | Rate limiting + caching |
| Jaeger | 1.58 | Distributed tracing |
| Prometheus | 2.53 | Metrics collection |
| Grafana | 11.1 | Metrics visualization |

## ⚡ Quick Start

### Prerequisites
- Docker Desktop with WSL2 integration
- Java 21, Maven 3.9+, Node.js 20+

### 1. Start Infrastructure
```bash
cd infra/docker-compose && docker compose up -d
```

### 2. Start Services
```bash
cd api-gateway-service && mvn spring-boot:run > /tmp/gateway.log 2>&1 &
cd ingestion-service && mvn spring-boot:run > /tmp/ingestion.log 2>&1 &
cd analytics-service && mvn spring-boot:run > /tmp/analytics.log 2>&1 &
cd fraud-detection-service && mvn spring-boot:run > /tmp/fraud.log 2>&1 &
cd ai-insight-service && mvn spring-boot:run > /tmp/insight.log 2>&1 &
```

### 3. Start Dashboard
```bash
cd sentinelflow-frontend && npm run dev
```

### 4. Open Dashboard
Navigate to [http://localhost:5173](http://localhost:5173)

## 🧪 Test the Pipeline

### Send a Legitimate Transaction
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-001",
    "amount": 1500,
    "currency": "INR",
    "merchantId": "amazon-india",
    "cardHash": "hash-001",
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "countryCode": "IN",
    "eventTime": "2026-01-01T00:00:00Z"
  }'
```

### Send a Fraud Transaction
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-FRAUD-001",
    "amount": 95000,
    "currency": "INR",
    "merchantId": "crypto-nigeria",
    "cardHash": "hash-002",
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "countryCode": "NG",
    "eventTime": "2026-01-01T00:00:00Z"
  }'
```

## 📊 Monitoring

| Tool | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3001 | admin/admin123 |
| Prometheus | http://localhost:9090 | — |
| Kafka UI | http://localhost:8090 | — |
| Jaeger | http://localhost:16686 | — |

## 🔧 Tech Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Cloud Gateway, Apache Kafka, PostgreSQL, Redis, Flyway

**Frontend:** React 19, TypeScript, Vite, Tailwind CSS

**Infrastructure:** Docker, Kubernetes-ready, GitHub Actions CI/CD, Prometheus, Grafana, Jaeger

**Author:** Priyanshu Verma | Java Backend Engineer | TCS
