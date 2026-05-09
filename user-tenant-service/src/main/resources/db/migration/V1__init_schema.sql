-- ═══════════════════════════════════════════════════════════════════════════
-- SentinelFlow V1 — Complete Database Schema
-- ═══════════════════════════════════════════════════════════════════════════

-- ── Enable extensions ────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ── 1. tenants ────────────────────────────────────────────────────────────────
CREATE TABLE tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    plan        VARCHAR(50)  NOT NULL DEFAULT 'free'
                             CHECK (plan IN ('free','pro','enterprise')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'active'
                             CHECK (status IN ('active','suspended','cancelled')),
    max_rps     INTEGER      NOT NULL DEFAULT 100,
    max_rps_burst INTEGER    NOT NULL DEFAULT 200,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug   ON tenants(slug);
CREATE INDEX idx_tenants_status ON tenants(status);

-- ── 2. users ──────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(60)  NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'viewer'
                                 CHECK (role IN ('admin','analyst','viewer')),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email     ON users(email);

-- ── 3. api_keys ───────────────────────────────────────────────────────────────
CREATE TABLE api_keys (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    key_hash    VARCHAR(60)  NOT NULL UNIQUE,
    key_prefix  VARCHAR(8)   NOT NULL,
    last_used_at TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ,
    is_revoked  BOOLEAN      NOT NULL DEFAULT false,
    revoked_at  TIMESTAMPTZ,
    revoked_by  UUID         REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_tenant_id  ON api_keys(tenant_id);
CREATE INDEX idx_api_keys_key_prefix ON api_keys(key_prefix);

-- ── 4. rate_limit_configs ─────────────────────────────────────────────────────
CREATE TABLE rate_limit_configs (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    endpoint_pattern VARCHAR(500),
    algorithm        VARCHAR(20)  NOT NULL DEFAULT 'sliding_window'
                                  CHECK (algorithm IN ('sliding_window','token_bucket','fixed_window')),
    max_requests     INTEGER      NOT NULL,
    window_seconds   INTEGER      NOT NULL,
    burst_capacity   INTEGER,
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rl_configs_tenant_id ON rate_limit_configs(tenant_id);

-- ── 5. rate_limit_events ──────────────────────────────────────────────────────
CREATE TABLE rate_limit_events (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID        NOT NULL REFERENCES tenants(id),
    api_key_id     UUID        REFERENCES api_keys(id),
    endpoint       VARCHAR(500) NOT NULL,
    method         VARCHAR(10)  NOT NULL,
    client_ip      INET,
    request_count  INTEGER      NOT NULL,
    limit_applied  INTEGER      NOT NULL,
    window_start   TIMESTAMPTZ  NOT NULL,
    breached_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rle_tenant_breached
    ON rate_limit_events(tenant_id, breached_at DESC);

-- ── 6. outbox ─────────────────────────────────────────────────────────────────
CREATE TABLE outbox (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID        NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    tenant_id      UUID        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
    ON outbox(created_at)
    WHERE published_at IS NULL;

-- ── 7. payment_events ─────────────────────────────────────────────────────────
CREATE TABLE payment_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    transaction_id VARCHAR(100) NOT NULL,
    card_hash      VARCHAR(64)  NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    currency       CHAR(3)      NOT NULL,
    merchant_id    VARCHAR(100) NOT NULL,
    merchant_cat   VARCHAR(10),
    country_code   CHAR(2),
    city           VARCHAR(100),
    event_time     TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, transaction_id)
);

CREATE INDEX idx_pe_tenant_card
    ON payment_events(tenant_id, card_hash, event_time DESC);
CREATE INDEX idx_pe_tenant_merchant
    ON payment_events(tenant_id, merchant_id);
CREATE INDEX idx_pe_event_time
    ON payment_events(event_time DESC);

-- ── 8. fraud_decisions ────────────────────────────────────────────────────────
CREATE TABLE fraud_decisions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenants(id),
    payment_event_id    UUID         NOT NULL REFERENCES payment_events(id),
    fraud_score         DECIMAL(5,4) NOT NULL,
    score_threshold     DECIMAL(5,4) NOT NULL,
    decision            VARCHAR(20)  NOT NULL
                                     CHECK (decision IN ('FRAUD','LEGITIMATE','REVIEW')),
    model_version       VARCHAR(50)  NOT NULL,
    model_variant       VARCHAR(20)  NOT NULL DEFAULT 'champion',
    feature_vector_hash VARCHAR(64),
    shap_values         JSONB,
    shap_available      BOOLEAN      NOT NULL DEFAULT false,
    analyst_decision    VARCHAR(20)  CHECK (analyst_decision IN ('CONFIRMED','FALSE_POSITIVE')),
    analyst_id          UUID         REFERENCES users(id),
    analyst_notes       TEXT,
    reviewed_at         TIMESTAMPTZ,
    scored_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fd_tenant_time
    ON fraud_decisions(tenant_id, scored_at DESC);
CREATE INDEX idx_fd_unreviewed
    ON fraud_decisions(tenant_id, scored_at DESC)
    WHERE analyst_decision IS NULL AND decision = 'FRAUD';

-- ── 9. fraud_case_embeddings ──────────────────────────────────────────────────
CREATE TABLE fraud_case_embeddings (
    id                UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID    NOT NULL REFERENCES tenants(id),
    fraud_decision_id UUID    NOT NULL REFERENCES fraud_decisions(id),
    case_summary      TEXT    NOT NULL,
    embedding         vector(1536),
    metadata          JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fce_tenant ON fraud_case_embeddings(tenant_id);

-- ── 10. llm_cost_log ──────────────────────────────────────────────────────────
CREATE TABLE llm_cost_log (
    id                UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID    NOT NULL REFERENCES tenants(id),
    fraud_decision_id UUID    REFERENCES fraud_decisions(id),
    model             VARCHAR(100) NOT NULL,
    tokens_in         INTEGER NOT NULL DEFAULT 0,
    tokens_out        INTEGER NOT NULL DEFAULT 0,
    cost_usd          DECIMAL(10,6) NOT NULL DEFAULT 0,
    cache_hit         BOOLEAN NOT NULL DEFAULT false,
    latency_ms        INTEGER,
    called_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lcl_tenant_date ON llm_cost_log(tenant_id, called_at DESC);

-- ── 11. api_metrics ───────────────────────────────────────────────────────────
CREATE TABLE api_metrics (
    bucket_time      TIMESTAMPTZ  NOT NULL,
    tenant_id        UUID         NOT NULL,
    endpoint         VARCHAR(500) NOT NULL,
    method           VARCHAR(10)  NOT NULL,
    request_count    BIGINT       NOT NULL DEFAULT 0,
    error_count      BIGINT       NOT NULL DEFAULT 0,
    p50_latency_ms   INTEGER,
    p95_latency_ms   INTEGER,
    p99_latency_ms   INTEGER,
    PRIMARY KEY (bucket_time, tenant_id, endpoint, method)
);

CREATE INDEX idx_api_metrics_tenant_time
    ON api_metrics(tenant_id, bucket_time DESC);

-- ── Row-Level Security ────────────────────────────────────────────────────────
ALTER TABLE tenants           ENABLE ROW LEVEL SECURITY;
ALTER TABLE users             ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_keys          ENABLE ROW LEVEL SECURITY;
ALTER TABLE rate_limit_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE rate_limit_events  ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_events    ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_decisions   ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_case_embeddings ENABLE ROW LEVEL SECURITY;
ALTER TABLE llm_cost_log      ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_metrics       ENABLE ROW LEVEL SECURITY;

-- RLS Policies — tenants see only their own data
CREATE POLICY tenant_isolation ON tenants
    USING (id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON api_keys
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON rate_limit_configs
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON rate_limit_events
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON payment_events
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON fraud_decisions
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON fraud_case_embeddings
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON llm_cost_log
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON api_metrics
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- ── Seed: default tenant for local dev ────────────────────────────────────────
INSERT INTO tenants (id, name, slug, plan, status, max_rps, max_rps_burst)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'SentinelFlow Dev',
    'sf-dev',
    'enterprise',
    'active',
    10000,
    20000
);

INSERT INTO users (id, tenant_id, email, password_hash, full_name, role)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'admin@sentinelflow.dev',
    '$2a$12$placeholder.hash.for.dev.only',
    'Dev Admin',
    'admin'
);
