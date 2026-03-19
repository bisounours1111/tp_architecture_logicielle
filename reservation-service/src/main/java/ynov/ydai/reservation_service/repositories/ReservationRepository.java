package ynov.ydai.reservation_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        select case when count(r) > 0 then true else false end
        from Reservation r
        where r.roomId = :roomId
          and r.status = :status
          and r.startDateTime < :endDateTime
          and r.endDateTime > :startDateTime
    """)
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("status") ReservationStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
