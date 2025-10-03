package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.pass.CreatePassDTO;
import com.packed_go.event_service.dtos.pass.PassDTO;
import com.packed_go.event_service.services.PassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-service/passes")
@RequiredArgsConstructor
@Slf4j
public class PassController {

    private final PassService passService;

    @PostMapping
    public ResponseEntity<PassDTO> createPass(@RequestBody CreatePassDTO createPassDTO) {
        log.info("Creando pass para evento: {}", createPassDTO.getEventId());
        PassDTO pass = passService.createPass(createPassDTO);
        return ResponseEntity.ok(pass);
    }

    @PostMapping("/event/{eventId}")
    public ResponseEntity<PassDTO> createPassForEvent(@PathVariable Long eventId, @RequestParam(required = false) String code) {
        log.info("Creando pass para evento: {} con código: {}", eventId, code);
        PassDTO pass = passService.createPassForEvent(eventId, code);
        return ResponseEntity.ok(pass);
    }

    @PutMapping("/{passId}/sell")
    public ResponseEntity<PassDTO> sellPass(@PathVariable Long passId, @RequestParam Long userId) {
        log.info("Vendiendo pass: {} a usuario: {}", passId, userId);
        PassDTO pass = passService.sellPass(passId, userId);
        return ResponseEntity.ok(pass);
    }

    @PutMapping("/code/{passCode}/sell")
    public ResponseEntity<PassDTO> sellPassByCode(@PathVariable String passCode, @RequestParam Long userId) {
        log.info("Vendiendo pass con código: {} a usuario: {}", passCode, userId);
        PassDTO pass = passService.sellPassByCode(passCode, userId);
        return ResponseEntity.ok(pass);
    }

    @GetMapping("/{passId}")
    public ResponseEntity<PassDTO> getPass(@PathVariable Long passId) {
        log.info("Obteniendo pass: {}", passId);
        PassDTO pass = passService.findById(passId);
        return ResponseEntity.ok(pass);
    }

    @GetMapping("/code/{passCode}")
    public ResponseEntity<PassDTO> getPassByCode(@PathVariable String passCode) {
        log.info("Obteniendo pass con código: {}", passCode);
        PassDTO pass = passService.findByCode(passCode);
        return ResponseEntity.ok(pass);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<PassDTO>> getPassesByEvent(@PathVariable Long eventId) {
        log.info("Obteniendo passes para evento: {}", eventId);
        List<PassDTO> passes = passService.findByEventId(eventId);
        return ResponseEntity.ok(passes);
    }

    @GetMapping("/event/{eventId}/available")
    public ResponseEntity<List<PassDTO>> getAvailablePassesByEvent(@PathVariable Long eventId) {
        log.info("Obteniendo passes disponibles para evento: {}", eventId);
        List<PassDTO> passes = passService.findAvailablePassesByEventId(eventId);
        return ResponseEntity.ok(passes);
    }

    @GetMapping("/event/{eventId}/sold")
    public ResponseEntity<List<PassDTO>> getSoldPassesByEvent(@PathVariable Long eventId) {
        log.info("Obteniendo passes vendidos para evento: {}", eventId);
        List<PassDTO> passes = passService.findSoldPassesByEventId(eventId);
        return ResponseEntity.ok(passes);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PassDTO>> getPassesByUser(@PathVariable Long userId) {
        log.info("Obteniendo passes para usuario: {}", userId);
        List<PassDTO> passes = passService.findByUserId(userId);
        return ResponseEntity.ok(passes);
    }

    @GetMapping("/event/{eventId}/available/count")
    public ResponseEntity<Long> getAvailablePassesCount(@PathVariable Long eventId) {
        log.info("Contando passes disponibles para evento: {}", eventId);
        Long count = passService.countAvailablePassesByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/event/{eventId}/sold/count")
    public ResponseEntity<Long> getSoldPassesCount(@PathVariable Long eventId) {
        log.info("Contando passes vendidos para evento: {}", eventId);
        Long count = passService.countSoldPassesByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/event/{eventId}/has-available")
    public ResponseEntity<Boolean> hasAvailablePasses(@PathVariable Long eventId) {
        log.info("Verificando si hay passes disponibles para evento: {}", eventId);
        boolean hasAvailable = passService.hasAvailablePasses(eventId);
        return ResponseEntity.ok(hasAvailable);
    }
}
