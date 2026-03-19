package ynov.ydai.reservation_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.entities.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByMemberIdAndStatus(Long memberId, ReservationStatus status);
    List<Reservation> findByRoomIdAndStatus(Long roomId, ReservationStatus status);
    List<Reservation> findByStatusAndEndDateTimeBefore(ReservationStatus status, LocalDateTime dateTime);
    void deleteByMemberId(Long memberId);
    boolean hasOverlappingConfirmedReservation(
            Long roomId,
            ReservationStatus status,
            LocalDateTime endDateTime,
            LocalDateTime startDateTime
    );
}
