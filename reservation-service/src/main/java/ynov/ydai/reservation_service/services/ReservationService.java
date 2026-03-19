package ynov.ydai.reservation_service.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ynov.ydai.reservation_service.clients.MemberClient;
import ynov.ydai.reservation_service.clients.RoomClient;
import ynov.ydai.reservation_service.dto.MemberDTO;
import ynov.ydai.reservation_service.dto.RoomDTO;
import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.entities.ReservationStatus;
import ynov.ydai.reservation_service.patterns.*;
import ynov.ydai.reservation_service.repositories.ReservationRepository;

import java.util.List;
import java.util.Optional;

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
        // 1. Vérifier la salle via Room Service (REST)
        RoomDTO room = roomClient.getRoomById(reservation.getRoomId());
        if (!room.isAvailable()) {
            throw new RuntimeException("Room is not available");
        }

        // 2. Vérifier le membre via Member Service (REST)
        MemberDTO member = memberClient.getMemberById(reservation.getMemberId());
        if (member.isSuspended()) {
            throw new RuntimeException("Member is suspended");
        }

        // 3. Créer la réservation
        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);

        // 4. Notifier Room Service (Kafka) pour rendre la salle indisponible
        kafkaTemplate.send("reservation-events-topic", "RESERVATION_CREATED:" + saved.getRoomId());

        // 5. Vérifier les quotas et notifier Member Service (Kafka) si besoin
        checkQuotasAndNotify(member);

        return saved;
    }

    @Transactional
    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        ReservationState state = getState(reservation);
        state.cancel(reservation);
        
        Reservation saved = reservationRepository.save(reservation);
        
        // Notifier les services
        kafkaTemplate.send("reservation-events-topic", "RESERVATION_CANCELLED:" + saved.getRoomId());
        
        MemberDTO member = memberClient.getMemberById(saved.getMemberId());
        checkQuotasAndNotify(member);
        
        return saved;
    }

    @Transactional
    public Reservation completeReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        ReservationState state = getState(reservation);
        state.complete(reservation);
        
        Reservation saved = reservationRepository.save(reservation);
        
        // Notifier les services
        kafkaTemplate.send("reservation-events-topic", "RESERVATION_COMPLETED:" + saved.getRoomId());
        
        MemberDTO member = memberClient.getMemberById(saved.getMemberId());
        checkQuotasAndNotify(member);
        
        return saved;
    }

    private void checkQuotasAndNotify(MemberDTO member) {
        List<Reservation> activeReservations = reservationRepository.findByMemberIdAndStatus(member.getId(), ReservationStatus.CONFIRMED);
        
        if (activeReservations.size() >= member.getMaxConcurrentBookings()) {
            kafkaTemplate.send("member-suspension-topic", "SUSPEND:" + member.getId());
        } else {
            kafkaTemplate.send("member-suspension-topic", "UNSUSPEND:" + member.getId());
        }
    }

    private ReservationState getState(Reservation reservation) {
        switch (reservation.getStatus()) {
            case CONFIRMED: return new ConfirmedState();
            case COMPLETED: return new CompletedState();
            case CANCELLED: return new CancelledState();
            default: throw new IllegalArgumentException("Unknown status");
        }
    }

    @KafkaListener(topics = "room-deleted-topic", groupId = "reservation-service-group")
    @Transactional
    public void handleRoomDeleted(String roomId) {
        List<Reservation> reservations = reservationRepository.findByRoomIdAndStatus(Long.parseLong(roomId), ReservationStatus.CONFIRMED);
        for (Reservation res : reservations) {
            res.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(res);
        }
    }

    @KafkaListener(topics = "member-deleted-topic", groupId = "reservation-service-group")
    @Transactional
    public void handleMemberDeleted(String memberId) {
        reservationRepository.deleteByMemberId(Long.parseLong(memberId));
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}
