package ynov.ydai.reservation_service.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ynov.ydai.reservation_service.clients.MemberClient;
import ynov.ydai.reservation_service.clients.RoomClient;
import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.entities.ReservationStatus;
import ynov.ydai.reservation_service.patterns.*;
import ynov.ydai.reservation_service.repositories.ReservationRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomClient roomClient;
    private final MemberClient memberClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReservationService(ReservationRepository reservationRepository, 
                              RoomClient roomClient, 
                              MemberClient memberClient, 
                              KafkaTemplate<String, String> kafkaTemplate) {
        this.reservationRepository = reservationRepository;
        this.roomClient = roomClient;
        this.memberClient = memberClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Reservation createReservation(Reservation reservation) {
        validateReservationDates(reservation);
        roomClient.getRoomById(reservation.getRoomId());
        validateRoomAvailabilityForSlot(reservation);

        if (memberClient.getMemberById(reservation.getMemberId()).isSuspended()) {
            throw new RuntimeException("Member is suspended");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);

        if (isActiveNow(saved, LocalDateTime.now())) {
            sendRoomEvent("RESERVATION_CREATED", saved.getRoomId());
        }
        refreshMemberSuspension(saved.getMemberId());

        return saved;
    }

    @Transactional
    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        boolean shouldReleaseRoom = hasStarted(reservation, LocalDateTime.now());

        ReservationState state = getState(reservation);
        state.cancel(reservation);

        Reservation saved = reservationRepository.save(reservation);

        if (shouldReleaseRoom) {
            sendRoomEvent("RESERVATION_CANCELLED", saved.getRoomId());
        }
        refreshMemberSuspension(saved.getMemberId());

        return saved;
    }

    @Transactional
    public Reservation completeReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        boolean shouldReleaseRoom = hasStarted(reservation, LocalDateTime.now());

        ReservationState state = getState(reservation);
        state.complete(reservation);

        Reservation saved = reservationRepository.save(reservation);

        if (shouldReleaseRoom) {
            sendRoomEvent("RESERVATION_COMPLETED", saved.getRoomId());
        }
        refreshMemberSuspension(saved.getMemberId());

        return saved;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void completeExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndEndDateTimeBefore(
                ReservationStatus.CONFIRMED,
                now
        );
        Set<Long> memberIdsToRefresh = new HashSet<>();

        for (Reservation reservation : expiredReservations) {
            ReservationState state = getState(reservation);
            state.complete(reservation);
            reservationRepository.save(reservation);
            sendRoomEvent("RESERVATION_COMPLETED", reservation.getRoomId());
            memberIdsToRefresh.add(reservation.getMemberId());
        }

        for (Long memberId : memberIdsToRefresh) {
            refreshMemberSuspension(memberId);
        }
    }

    private void refreshMemberSuspension(Long memberId) {
        try {
            var member = memberClient.getMemberById(memberId);
            List<Reservation> activeReservations = reservationRepository.findByMemberIdAndStatus(
                    member.getId(),
                    ReservationStatus.CONFIRMED
            );

            if (activeReservations.size() >= member.getMaxConcurrentBookings()) {
                kafkaTemplate.send("member-suspension-topic", "SUSPEND:" + member.getId());
            } else {
                kafkaTemplate.send("member-suspension-topic", "UNSUSPEND:" + member.getId());
            }
        } catch (Exception ignored) {
            // Le membre a pu être supprimé ou temporairement indisponible.
        }
    }

    private void validateReservationDates(Reservation reservation) {
        if (reservation.getStartDateTime() == null || reservation.getEndDateTime() == null) {
            throw new RuntimeException("Reservation dates are required");
        }
        if (!reservation.getStartDateTime().isBefore(reservation.getEndDateTime())) {
            throw new RuntimeException("Reservation end date must be after start date");
        }
        if (!reservation.getEndDateTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Reservation end date must be in the future");
        }
    }

    private void validateRoomAvailabilityForSlot(Reservation reservation) {
        boolean overlapExists = reservationRepository.existsOverlappingReservation(
                reservation.getRoomId(),
                ReservationStatus.CONFIRMED,
                reservation.getStartDateTime(),
                reservation.getEndDateTime()
        );

        if (overlapExists) {
            throw new RuntimeException("Room is already booked for the selected time slot");
        }
    }

    private boolean isActiveNow(Reservation reservation, LocalDateTime now) {
        return hasStarted(reservation, now) && reservation.getEndDateTime().isAfter(now);
    }

    private boolean hasStarted(Reservation reservation, LocalDateTime now) {
        return !reservation.getStartDateTime().isAfter(now);
    }

    private void sendRoomEvent(String action, Long roomId) {
        kafkaTemplate.send("reservation-events-topic", action + ":" + roomId);
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @KafkaListener(topics = "room-deleted-topic", groupId = "reservation-service-group")
    @Transactional
    public void handleRoomDeleted(String roomId) {
        List<Reservation> reservations = reservationRepository.findByRoomIdAndStatus(
                Long.parseLong(roomId),
                ReservationStatus.CONFIRMED
        );
        Set<Long> memberIdsToRefresh = new HashSet<>();

        for (Reservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            memberIdsToRefresh.add(reservation.getMemberId());
        }

        for (Long memberId : memberIdsToRefresh) {
            refreshMemberSuspension(memberId);
        }
    }

    @KafkaListener(topics = "member-deleted-topic", groupId = "reservation-service-group")
    @Transactional
    public void handleMemberDeleted(String memberId) {
        reservationRepository.deleteByMemberId(Long.parseLong(memberId));
    }

    private ReservationState getState(Reservation reservation) {
        switch (reservation.getStatus()) {
            case CONFIRMED: return new ConfirmedState();
            case COMPLETED: return new CompletedState();
            case CANCELLED: return new CancelledState();
            default: throw new IllegalArgumentException("Unknown status");
        }
    }
}
