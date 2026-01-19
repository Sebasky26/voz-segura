package com.vozsegura.vozsegura.controller.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para verificar configuración y salud del sistema
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${didit.app-id:NOT_SET}")
    private String diditAppId;

    @Value("${didit.api-key:NOT_SET}")
    private String diditApiKey;

    @Value("${didit.workflow-id:NOT_SET}")
    private String diditWorkflowId;

    @Value("${didit.api-url:NOT_SET}")
    private String diditApiUrl;

    @Value("${didit.webhook-url:NOT_SET}")
    private String diditWebhookUrl;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("didit", Map.of(
            "appId", diditAppId.substring(0, Math.min(10, diditAppId.length())) + "...",
            "apiKeySet", !diditApiKey.equals("NOT_SET"),
            "workflowId", diditWorkflowId.substring(0, Math.min(10, diditWorkflowId.length())) + "...",
            "apiUrl", diditApiUrl,
            "webhookUrl", diditWebhookUrl
        ));
        
        config.put("status", "OK");
        return config;
    }

    @GetMapping("/didit-debug")
    public Map<String, Object> diditDebug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("didit_app_id", diditAppId);
        debug.put("didit_api_key_set", !diditApiKey.equals("NOT_SET"));
        debug.put("didit_workflow_id", diditWorkflowId);
        debug.put("didit_api_url", diditApiUrl);
        debug.put("didit_webhook_url", diditWebhookUrl);
        
        log.info("Didit Config Debug: {}", debug);
        
        return debug;
    }
}
