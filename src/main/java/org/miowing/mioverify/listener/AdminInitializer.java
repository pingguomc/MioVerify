package org.miowing.mioverify.listener;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Admin user initializer
 */
@Component
@ConditionalOnProperty(name = "mioverify.admin.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class AdminInitializer {
    
    @Autowired
    private AdminService adminService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdmin() {
        log.info("Initializing admin users...");
        adminService.initializeDefaultAdmin();
        log.info("Admin initialization completed.");
    }
}
