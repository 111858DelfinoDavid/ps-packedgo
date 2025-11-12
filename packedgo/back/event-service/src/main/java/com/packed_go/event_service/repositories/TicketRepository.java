package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Buscar tickets por usuario con pass eager loaded
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.pass WHERE t.userId = :userId")
    List<Ticket> findByUserId(@Param("userId") Long userId);

    // Buscar tickets por usuario y activos
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.pass WHERE t.userId = :userId AND t.active = true")
    List<Ticket> findByUserIdAndActiveTrue(@Param("userId") Long userId);

    // Buscar ticket por pass
    Optional<Ticket> findByPass_Id(Long passId);

    // Buscar ticket por pass con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.pass.id = :passId")
    Optional<Ticket> findByPass_IdWithLock(@Param("passId") Long passId);

    // Buscar tickets canjeados por usuario
    List<Ticket> findByUserIdAndRedeemedTrue(Long userId);

    // Buscar tickets no canjeados por usuario
    List<Ticket> findByUserIdAndRedeemedFalse(Long userId);

    // Buscar tickets por evento (a través del pass)
    @Query("SELECT t FROM Ticket t WHERE t.pass.event.id = :eventId")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);

    // Contar tickets vendidos por evento
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.pass.event.id = :eventId")
    Long countTicketsByEventId(@Param("eventId") Long eventId);

    // Contar tickets canjeados por evento
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.pass.event.id = :eventId AND t.redeemed = true")
    Long countRedeemedTicketsByEventId(@Param("eventId") Long eventId);

    // Buscar ticket por código de pass
    @Query("SELECT t FROM Ticket t WHERE t.pass.code = :passCode")
    Optional<Ticket> findByPassCode(@Param("passCode") String passCode);

    // Buscar ticket por código de pass con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.pass.code = :passCode")
    Optional<Ticket> findByPassCodeWithLock(@Param("passCode") String passCode);

    // Buscar ticket por ID con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(@Param("id") Long id);
}
