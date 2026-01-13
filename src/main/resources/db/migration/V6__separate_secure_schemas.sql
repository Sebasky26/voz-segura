-- Crear esquemas separados para datos sensibles
CREATE SCHEMA IF NOT EXISTS secure_identities;
CREATE SCHEMA IF NOT EXISTS evidence_vault;
CREATE SCHEMA IF NOT EXISTS audit_logs;

-- Mover tabla identity_vault a esquema seguro (solo si existe en public)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'identity_vault') THEN
        ALTER TABLE identity_vault SET SCHEMA secure_identities;
    END IF;
END $$;

-- Mover tabla evidence a esquema de evidencias (solo si existe en public)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'evidence') THEN
        ALTER TABLE evidence SET SCHEMA evidence_vault;
    END IF;
END $$;

-- Mover tabla audit_event a esquema de logs (solo si existe en public)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'audit_event') THEN
        ALTER TABLE audit_event SET SCHEMA audit_logs;
    END IF;
END $$;

-- Habilitar Row Level Security en todas las tablas sensibles (si existen)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'secure_identities' AND tablename = 'identity_vault') THEN
        ALTER TABLE secure_identities.identity_vault ENABLE ROW LEVEL SECURITY;
        -- Políticas RLS para identity_vault
        DROP POLICY IF EXISTS "Service role only access" ON secure_identities.identity_vault;
        CREATE POLICY "Service role only access" ON secure_identities.identity_vault
            FOR ALL
            USING (current_user = 'postgres' OR current_user LIKE 'service_role%');
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'evidence_vault' AND tablename = 'evidence') THEN
        ALTER TABLE evidence_vault.evidence ENABLE ROW LEVEL SECURITY;
        -- Políticas RLS para evidence
        DROP POLICY IF EXISTS "Service role only access" ON evidence_vault.evidence;
        CREATE POLICY "Service role only access" ON evidence_vault.evidence
            FOR ALL
            USING (current_user = 'postgres' OR current_user LIKE 'service_role%');
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'audit_logs' AND tablename = 'audit_event') THEN
        ALTER TABLE audit_logs.audit_event ENABLE ROW LEVEL SECURITY;
        -- Políticas RLS para audit_event
        DROP POLICY IF EXISTS "Service role only access" ON audit_logs.audit_event;
        CREATE POLICY "Service role only access" ON audit_logs.audit_event
            FOR ALL
            USING (current_user = 'postgres' OR current_user LIKE 'service_role%');
    END IF;
END $$;

-- Crear índices para búsquedas (si las tablas existen)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'secure_identities' AND tablename = 'identity_vault') THEN
        CREATE INDEX IF NOT EXISTS idx_identity_vault_citizen_hash ON secure_identities.identity_vault(citizen_hash);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'evidence_vault' AND tablename = 'evidence') THEN
        CREATE INDEX IF NOT EXISTS idx_evidence_complaint_id ON evidence_vault.evidence(complaint_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'audit_logs' AND tablename = 'audit_event') THEN
        CREATE INDEX IF NOT EXISTS idx_audit_event_time ON audit_logs.audit_event(event_time);
        CREATE INDEX IF NOT EXISTS idx_audit_tracking_id ON audit_logs.audit_event(tracking_id);
    END IF;
END $$;
