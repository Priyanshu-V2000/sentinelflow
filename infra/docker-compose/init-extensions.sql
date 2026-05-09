-- Enable pgvector for AI embeddings
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Confirm
SELECT extname, extversion FROM pg_extension
WHERE extname IN ('uuid-ossp', 'vector');
