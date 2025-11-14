package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import com.packed_go.event_service.security.JwtTokenValidator;
import com.packed_go.event_service.services.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event-service/event")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    private final EventService service;
    private final ModelMapper modelMapper;
    private final JwtTokenValidator jwtValidator;

    /**
     * 🔐 Helper: Extrae userId del JWT
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        return jwtValidator.getUserIdFromToken(token);
    }

    /**
     * 🔓 GET /event/{id} - Obtener evento por ID (público para ORDER-SERVICE)
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * 🔓 GET /event - Listar todos los eventos (público para consumers)
     * TODO: En producción, considerar filtrar solo eventos activos o públicos
     */
    @GetMapping
    public ResponseEntity<List<EventDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * 🔒 GET /event/my-events - Listar solo los eventos del admin autenticado
     */
    @GetMapping("/my-events")
    public ResponseEntity<List<EventDTO>> getMyEvents(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} fetching their own events", userId);
        
        List<EventDTO> myEvents = service.findAll().stream()
                .filter(event -> event.getCreatedBy().equals(userId))
                .toList();
        
        return ResponseEntity.ok(myEvents);
    }

    /**
     * 🔒 POST /event - Crear evento (valida que createdBy del body == userId del JWT)
     */
    @PostMapping
    public ResponseEntity<EventDTO> create(
            @RequestBody CreateEventDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} creating event: {}", userId, dto.getName());
        
        // Validar que el createdBy del body coincide con el userId del JWT
        if (dto.getCreatedBy() == null) {
            // Si no viene en el body, lo inyectamos desde JWT
            dto.setCreatedBy(userId);
        } else if (!dto.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to create event with different createdBy: {}", userId, dto.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        EventDTO created = service.createEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 🔒 PUT /event/{id} - Actualizar evento (valida ownership)
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(
            @PathVariable Long id, 
            @RequestBody EventDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} updating event {}", userId, id);
        
        // Validar ownership
        EventDTO existingEvent = service.findById(id);
        if (!existingEvent.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to update event {} owned by {}", userId, id, existingEvent.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(service.updateEvent(id, dto));
    }

    /**
     * 🔒 DELETE /event/{id} - Eliminar evento físicamente (valida ownership)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deleting event {}", userId, id);
        
        // Validar ownership
        EventDTO existingEvent = service.findById(id);
        if (!existingEvent.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to delete event {} owned by {}", userId, id, existingEvent.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔒 DELETE /event/logical/{id} - Desactivar evento (valida ownership)
     */
    @DeleteMapping("/logical/{id}")
    public ResponseEntity<EventCategoryDTO> deleteLogical(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deactivating event {}", userId, id);
        
        // Validar ownership
        EventDTO existingEvent = service.findById(id);
        if (!existingEvent.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to deactivate event {} owned by {}", userId, id, existingEvent.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(modelMapper.map(service.deleteLogical(id), EventCategoryDTO.class));
    }

    /**
     * 🔓 GET /event/{eventId}/consumptions - Obtener consumiciones de un evento (público)
     */
    @GetMapping("/{eventId}/consumptions")
    public ResponseEntity<List<ConsumptionDTO>> getEventConsumptions(@PathVariable Long eventId) {
        List<ConsumptionDTO> consumptions = service.getEventConsumptions(eventId);
        return ResponseEntity.ok(consumptions);
    }

    /**
     * 🔒 POST /event/{id}/image - Subir imagen para un evento (valida ownership)
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            log.info("🔒 User {} uploading image for event {}", userId, id);
            
            // Validar ownership
            EventDTO existingEvent = service.findById(id);
            if (!existingEvent.getCreatedBy().equals(userId)) {
                log.warn("⚠️ User {} tried to upload image for event {} owned by {}", userId, id, existingEvent.getCreatedBy());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("image/png") && 
                 !contentType.equals("image/jpeg") && 
                 !contentType.equals("image/jpg"))) {
                return ResponseEntity.badRequest().body("Solo se permiten imágenes PNG, JPG o JPEG");
            }
            
            // Validar tamaño (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("La imagen no puede superar los 5MB");
            }
            
            service.saveEventImage(id, file.getBytes(), contentType);
            return ResponseEntity.ok().body(java.util.Map.of("message", "Imagen subida correctamente"));
            
        } catch (Exception e) {
            log.error("Error uploading image for event {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la imagen: " + e.getMessage());
        }
    }

    /**
     * 🔓 GET /event/{id}/image - Obtener imagen de un evento (público)
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        try {
            EventDTO event = service.findById(id);
            byte[] imageData = service.getEventImage(id);
            
            if (imageData == null || imageData.length == 0) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", event.getImageContentType())
                    .body(imageData);
                    
        } catch (Exception e) {
            log.error("Error getting image for event {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
