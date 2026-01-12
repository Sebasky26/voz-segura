-- Crear esquemas separados para datos sensibles
CREATE SCHEMA IF NOT EXISTS secure_identities;
CREATE SCHEMA IF NOT EXISTS evidence_vault;
CREATE SCHEMA IF NOT EXISTS audit_logs;

-- Mover tabla identity_vault a esquema seguro
ALTER TABLE identity_vault SET SCHEMA secure_identities;

-- Mover tabla evidence a esquema de evidencias
ALTER TABLE evidence SET SCHEMA evidence_vault;

-- Mover tabla audit_event a esquema de logs
ALTER TABLE audit_event SET SCHEMA audit_logs;

-- Habilitar Row Level Security en todas las tablas sensibles
ALTER TABLE secure_identities.identity_vault ENABLE ROW LEVEL SECURITY;
ALTER TABLE evidence_vault.evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs.audit_event ENABLE ROW LEVEL SECURITY;

-- Políticas RLS: Solo la aplicación puede acceder (usando service_role)
-- Para identity_vault
CREATE POLICY "Service role only access" ON secure_identities.identity_vault
    FOR ALL
    USING (auth.jwt() ->> 'role' = 'service_role');

-- Para evidence
CREATE POLICY "Service role only access" ON evidence_vault.evidence
    FOR ALL
    USING (auth.jwt() ->> 'role' = 'service_role');

-- Para audit_event (lectura más restringida)
CREATE POLICY "Service role only access" ON audit_logs.audit_event
    FOR ALL
    USING (auth.jwt() ->> 'role' = 'service_role');

-- Crear índices para búsquedas
CREATE INDEX idx_identity_vault_citizen_hash ON secure_identities.identity_vault(citizen_hash);
CREATE INDEX idx_evidence_complaint_id ON evidence_vault.evidence(complaint_id);
CREATE INDEX idx_audit_event_time ON audit_logs.audit_event(event_time);
CREATE INDEX idx_audit_tracking_id ON audit_logs.audit_event(tracking_id);
