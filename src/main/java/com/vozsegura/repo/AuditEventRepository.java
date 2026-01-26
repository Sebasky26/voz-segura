package com.vozsegura.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vozsegura.domain.entity.AuditEvent;

/**
 * Repositorio para gestionar registros de auditoria del sistema.
 * 
 * Proposito:
 * - Mantener un historial completo de eventos en el sistema (sin PII)
 * - Soportar consultas de auditoria para cumplimiento legal
 * - Permitir busquedas por tipo de evento y paginacion
 * - Auditar acciones de usuarios, cambios de denuncias, accesos administrativos
 * 
 * Seguridad (Zero Trust):
 * - actorUsername se guarda hasheado (imposible identificar usuario de que acciones hizo)
 * - eventType esta restringido a valores predefinidos (COMPLAINT_CREATED, ACCESS_DENIED, etc.)
 * - details se trunca a 512 caracteres para evitar PII accidental
 * - eventTime es de auditoria (NO puede ser editado por usuario, generado por BD)
 * 
 * Integracion:
 * - Usado por AuditService para registrar eventos
 * - Usado por AdminController para generar reportes de auditoria
 * - Usado por ComplianceService para cumplimiento normativo (GDPR, LOD)
 * 
 * Notas:
 * - Los registros nunca se borran (append-only log)
 * - Se asume que una tarea batch archiva eventos viejos (>2 anos) a cold storage
 * - La busqueda por actorUsername haseado es imposible (por diseno: anonimidad total)
 * 
 * @see com.vozsegura.domain.entity.AuditEvent
 * @see com.vozsegura.service.AuditService
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Obtiene todos los eventos de auditoria ordenados por tiempo descendente (mas recientes primero).
     * 
     * Proposito:
     * - Permitir visualizacion del historico completo de eventos
     * - Paginado para evitar queries demasiado grandes
     * - Util para reportes globales y dashboards de auditoria
     * 
     * @param pageable Especificacion de paginacion (page number, page size, sort)
     * @return Page de AuditEvent ordenados por eventTime DESC (mas recientes primero)
     * 
     * Ejemplo:
     * Page<AuditEvent> page = repo.findAllByOrderByEventTimeDesc(PageRequest.of(0, 50));
     * // Retorna los 50 eventos mas recientes
     */
    Page<AuditEvent> findAllByOrderByEventTimeDesc(Pageable pageable);

    /**
     * Obtiene lista DISTINCT de todos los tipos de eventos registrados.
     *
     * Proposito:
     * - Poblar el filtro de event_type en el panel de admin
     * - Mostrar solo los tipos que existen realmente en BD
     * - Evitar hardcodear valores que pueden quedar obsoletos
     *
     * @return Lista de event_type únicos ordenados alfabéticamente
     */
    @Query("SELECT DISTINCT e.eventType FROM AuditEvent e ORDER BY e.eventType ASC")
    List<String> findDistinctEventTypes();

    /**
     * Obtiene eventos de auditoria filtrados por tipo de evento.
     * 
     * Proposito:
     * - Busqueda especifica por tipo de evento (ej: COMPLAINT_CREATED, ADMIN_LOGIN_FAILED)
     * - Permite analisis de patrones de ataque (muchos LOGIN_FAILED = posible brute-force)
     * - Util para investigaciones de seguridad
     * 
     * Valores validos de eventType (definidos en SystemConfigService):
     * - COMPLAINT_CREATED: Nueva denuncia creada
     * - COMPLAINT_DERIVED: Denuncia derivada a otra entidad
     * - ADMIN_LOGIN: Login administrativo exitoso
     * - ADMIN_LOGIN_FAILED: Intento de login administrativo fallido
     * - IDENTITY_REVEAL: Revelacion excepcional de identidad
     * - ACCESS_DENIED: Acceso denegado (RBAC)
     * - CONFIG_CHANGED: Cambio de configuracion del sistema
     * - Y otros segun SystemConfig.configGroup='EVENT_TYPE'
     * 
     * @param eventType Tipo de evento a buscar (ej: "ADMIN_LOGIN_FAILED")
     * @param pageable Especificacion de paginacion
     * @return Page de AuditEvent del tipo especificado, ordenados por eventTime DESC
     * 
     * Ejemplo:
     * Page<AuditEvent> failedLogins = repo.findByEventType("ADMIN_LOGIN_FAILED", PageRequest.of(0, 20));
     * // Retorna los 20 intentos de login fallidos mas recientes
     */
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);
}
