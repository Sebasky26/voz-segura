-- =============================================================================
-- V8__fix_cross_schema_references.sql
-- Corregir referencias entre esquemas después de la separación
-- =============================================================================

-- Agregar constraint de foreign key explícito con referencia de esquema
-- Solo si la tabla evidence existe en evidence_vault
DO $$
BEGIN
    -- Verificar si la columna complaint_id tiene la constraint correcta
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'evidence_vault' AND tablename = 'evidence') THEN
        -- Eliminar constraint antigua si existe
        ALTER TABLE evidence_vault.evidence DROP CONSTRAINT IF EXISTS evidence_complaint_id_fkey;

        -- Agregar constraint con referencia explícita al esquema public
        ALTER TABLE evidence_vault.evidence
            ADD CONSTRAINT evidence_complaint_id_fkey
            FOREIGN KEY (complaint_id) REFERENCES public.complaint(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Asegurar que identity_vault tiene la constraint correcta desde complaint
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'secure_identities' AND tablename = 'identity_vault') THEN
        -- Eliminar constraint antigua si existe
        ALTER TABLE public.complaint DROP CONSTRAINT IF EXISTS complaint_identity_vault_id_fkey;

        -- Agregar constraint con referencia explícita al esquema secure_identities
        ALTER TABLE public.complaint
            ADD CONSTRAINT complaint_identity_vault_id_fkey
            FOREIGN KEY (identity_vault_id) REFERENCES secure_identities.identity_vault(id);
    END IF;
END $$;

-- Crear índices para mejorar rendimiento de joins
CREATE INDEX IF NOT EXISTS idx_complaint_identity_vault ON public.complaint(identity_vault_id);
CREATE INDEX IF NOT EXISTS idx_complaint_tracking_id ON public.complaint(tracking_id);
CREATE INDEX IF NOT EXISTS idx_complaint_status ON public.complaint(status);
CREATE INDEX IF NOT EXISTS idx_complaint_created_at ON public.complaint(created_at);

-- Comentarios de documentación
COMMENT ON TABLE public.complaint IS 'Denuncias cifradas - El texto está encriptado con AES-256';
COMMENT ON COLUMN public.complaint.encrypted_text IS 'Contenido de denuncia cifrado con clave derivada de AWS KMS';
COMMENT ON COLUMN public.complaint.tracking_id IS 'UUID único para seguimiento público - No revela identidad';
