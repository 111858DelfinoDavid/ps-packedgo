package com.packed_go.auth_service.config;

import com.packed_go.auth_service.services.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermissionService permissionService;

    @Override
    public void run(String... args) throws Exception {
        //log.info("Starting data initialization...");
        
        try {
            permissionService.initializeDefaultPermissions();
            //log.info("Data initialization completed successfully");
        } catch (Exception e) {
            //log.error("Error during data initialization", e);
        }
    }
}