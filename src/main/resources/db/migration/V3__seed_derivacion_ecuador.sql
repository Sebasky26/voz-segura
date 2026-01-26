-- Migración: Poblar tablas de derivación con datos reales de Ecuador
-- Versión: V2__seed_derivacion_ecuador.sql
-- Fecha: 2026-01-25

-- =========================================================
-- 1) ENTIDADES DESTINO (Instituciones reales de Ecuador)
-- =========================================================

-- Fiscalía General del Estado
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'FGE',
    'Fiscalía General del Estado',
    'Institución encargada de investigar delitos penales y ejercer la acción penal pública',
    true,
    'https://www.fiscalia.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Contraloría General del Estado
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'CGE',
    'Contraloría General del Estado',
    'Control de uso de recursos públicos y auditorías',
    true,
    'https://www.contraloria.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Defensoría del Pueblo
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'DPE',
    'Defensoría del Pueblo',
    'Protección de derechos humanos y ciudadanos',
    true,
    'https://www.dpe.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Ministerio de Trabajo
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'MDT',
    'Ministerio de Trabajo',
    'Casos laborales, acoso laboral, despidos injustificados',
    true,
    'https://www.trabajo.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Superintendencia de Compañías
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'SUPERCIAS',
    'Superintendencia de Compañías',
    'Fraudes corporativos, irregularidades en empresas',
    true,
    'https://www.supercias.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Ministerio de Salud
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'MSP',
    'Ministerio de Salud Pública',
    'Mala praxis médica, negligencias en salud',
    true,
    'https://www.salud.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Ministerio de Educación
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'MINEDUC',
    'Ministerio de Educación',
    'Acoso escolar, irregularidades en instituciones educativas',
    true,
    'https://www.educacion.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- SRI (Servicio de Rentas Internas)
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'SRI',
    'Servicio de Rentas Internas',
    'Evasión fiscal, fraude tributario',
    true,
    'https://www.sri.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Superintendencia de Bancos
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'SB',
    'Superintendencia de Bancos',
    'Fraudes bancarios, lavado de activos',
    true,
    'https://www.superbancos.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- Policía Nacional (Unidad Anti-Corrupción)
INSERT INTO reglas_derivacion.entidad_destino (code, name, description, active, endpoint_url)
VALUES (
    'UAFE',
    'Unidad de Análisis Financiero y Económico',
    'Lavado de activos, financiamiento ilícito',
    true,
    'https://www.uafe.gob.ec'
) ON CONFLICT (code) DO NOTHING;

-- =========================================================
-- 2) POLÍTICA DE DERIVACIÓN ACTIVA
-- =========================================================

INSERT INTO reglas_derivacion.politica_derivacion (
    name,
    legal_framework,
    version,
    effective_from,
    active
) VALUES (
    'Política Nacional de Derivación de Denuncias 2026',
    'Basada en: Código Orgánico Integral Penal (COIP), Ley Orgánica de la Contraloría, Código de Trabajo, Ley de Protección de Datos',
    'v1.0',
    '2026-01-01',
    true
) ON CONFLICT (version) DO NOTHING;

-- =========================================================
-- 3) REGLAS DE DERIVACIÓN
-- =========================================================

-- Obtener el ID de la política activa
DO $$
DECLARE
    v_policy_id bigint;
    v_dest_fge bigint;
    v_dest_cge bigint;
    v_dest_dpe bigint;
    v_dest_mdt bigint;
    v_dest_supercias bigint;
    v_dest_msp bigint;
    v_dest_mineduc bigint;
    v_dest_sri bigint;
    v_dest_sb bigint;
    v_dest_uafe bigint;
BEGIN
    -- Obtener IDs
    SELECT id INTO v_policy_id FROM reglas_derivacion.politica_derivacion WHERE version = 'v1.0';
    SELECT id INTO v_dest_fge FROM reglas_derivacion.entidad_destino WHERE code = 'FGE';
    SELECT id INTO v_dest_cge FROM reglas_derivacion.entidad_destino WHERE code = 'CGE';
    SELECT id INTO v_dest_dpe FROM reglas_derivacion.entidad_destino WHERE code = 'DPE';
    SELECT id INTO v_dest_mdt FROM reglas_derivacion.entidad_destino WHERE code = 'MDT';
    SELECT id INTO v_dest_supercias FROM reglas_derivacion.entidad_destino WHERE code = 'SUPERCIAS';
    SELECT id INTO v_dest_msp FROM reglas_derivacion.entidad_destino WHERE code = 'MSP';
    SELECT id INTO v_dest_mineduc FROM reglas_derivacion.entidad_destino WHERE code = 'MINEDUC';
    SELECT id INTO v_dest_sri FROM reglas_derivacion.entidad_destino WHERE code = 'SRI';
    SELECT id INTO v_dest_sb FROM reglas_derivacion.entidad_destino WHERE code = 'SB';
    SELECT id INTO v_dest_uafe FROM reglas_derivacion.entidad_destino WHERE code = 'UAFE';

    -- REGLA 1: Corrupción + Crítica = Fiscalía (URGENTE)
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Corrupción Crítica → Fiscalía',
        'Casos de corrupción de alta gravedad requieren investigación penal inmediata',
        true,
        'CORRUPCION',
        'CRITICA',
        v_dest_fge,
        10, -- Máxima prioridad
        false,
        24, -- 24 horas
        'COIP Art. 280-298 (Delitos contra la administración pública)',
        '{"keywords": ["soborno", "cohecho", "malversación", "peculado"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 2: Fraude Financiero + Crítica = UAFE
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Fraude Financiero Crítico → UAFE',
        'Lavado de activos o fraudes financieros graves',
        true,
        'FRAUDE',
        'CRITICA',
        v_dest_uafe,
        20,
        false,
        24,
        'Ley de Prevención de Lavado de Activos',
        '{"keywords": ["lavado", "transferencias", "cuentas offshore", "testaferros"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 3: Acoso Laboral → Ministerio de Trabajo
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Acoso Laboral → Ministerio de Trabajo',
        'Casos de acoso laboral, mobbing, despidos injustificados',
        true,
        'ACOSO_LABORAL',
        null, -- Cualquier severidad
        v_dest_mdt,
        50,
        false,
        72, -- 3 días
        'Código de Trabajo Art. 42',
        '{"keywords": ["despido", "mobbing", "hostigamiento", "discriminación"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 4: Mala Praxis Médica → Ministerio de Salud
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Mala Praxis Médica → MSP',
        'Negligencias médicas, mala praxis en instituciones de salud',
        true,
        'MALA_PRAXIS',
        null,
        v_dest_msp,
        40,
        true, -- Requiere revisión manual por complejidad técnica
        48,
        'Ley Orgánica de Salud',
        '{"keywords": ["negligencia", "cirugía", "diagnóstico erróneo", "hospital"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 5: Evasión Fiscal → SRI
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Evasión Fiscal → SRI',
        'Evasión de impuestos, facturas falsas, empresas fantasma',
        true,
        'FRAUDE',
        null,
        v_dest_sri,
        30,
        false,
        96, -- 4 días
        'Código Tributario',
        '{"keywords": ["impuestos", "facturas", "IVA", "renta", "evasión"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 6: Abuso de Poder (cualquier severidad) → Defensoría del Pueblo
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Abuso de Poder → Defensoría del Pueblo',
        'Violaciones de derechos humanos, abusos de autoridad',
        true,
        'ABUSO_PODER',
        null,
        v_dest_dpe,
        35,
        false,
        48,
        'Ley Orgánica de la Defensoría del Pueblo',
        '{"keywords": ["derechos humanos", "abuso", "autoridad", "policía"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 7: Fraude Corporativo → Superintendencia de Compañías
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Fraude Corporativo → Superintendencia de Compañías',
        'Irregularidades en sociedades, fraudes en juntas directivas',
        true,
        'FRAUDE',
        null,
        v_dest_supercias,
        45,
        true,
        120, -- 5 días
        'Ley de Compañías',
        '{"keywords": ["sociedades", "accionistas", "junta directiva", "balance"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 8: Acoso Escolar → Ministerio de Educación
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Acoso Escolar → Ministerio de Educación',
        'Bullying, acoso sexual en instituciones educativas',
        true,
        'ACOSO_ESCOLAR',
        null,
        v_dest_mineduc,
        25,
        false,
        48,
        'Reglamento General de Instituciones Educativas',
        '{"keywords": ["bullying", "escuela", "colegio", "estudiante", "profesor"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 9: Malversación de Fondos Públicos → Contraloría
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Malversación de Fondos Públicos → Contraloría',
        'Uso indebido de recursos del Estado, contrataciones irregulares',
        true,
        'CORRUPCION',
        null,
        v_dest_cge,
        15,
        false,
        48,
        'Ley Orgánica de la Contraloría General del Estado',
        '{"keywords": ["fondos públicos", "contratación", "licitación", "auditoría"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

    -- REGLA 10: Fraude Bancario → Superintendencia de Bancos
    INSERT INTO reglas_derivacion.regla_derivacion (
        policy_id, name, description, active,
        complaint_type_match, severity_match,
        destination_id, priority_order,
        requires_manual_review, sla_hours, normative_reference,
        conditions
    ) VALUES (
        v_policy_id,
        'Fraude Bancario → Superintendencia de Bancos',
        'Estafas bancarias, créditos irregulares, seguros fraudulentos',
        true,
        'FRAUDE',
        null,
        v_dest_sb,
        30,
        true,
        72,
        'Ley General de Instituciones del Sistema Financiero',
        '{"keywords": ["banco", "crédito", "cuenta", "tarjeta", "seguro"]}'::jsonb
    ) ON CONFLICT DO NOTHING;

END $$;
