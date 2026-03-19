package ynov.ydai.room_service.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ynov.ydai.room_service.entities.Room;
import ynov.ydai.room_service.repositories.RoomRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public RoomService(RoomRepository roomRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.roomRepository = roomRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String ROOM_DELETED_TOPIC = "room-deleted-topic";

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        
        room.setName(roomDetails.getName());
        room.setCity(roomDetails.getCity());
        room.setCapacity(roomDetails.getCapacity());
        room.setType(roomDetails.getType());
        room.setHourlyRate(roomDetails.getHourlyRate());
        room.setAvailable(roomDetails.isAvailable());
        
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        if (roomRepository.existsById(id)) {
            roomRepository.deleteById(id);
            // Kafka: Envoi asynchrone avec gestion d'erreur pour ne pas bloquer si Kafka est éteint
            try {
                kafkaTemplate.send(ROOM_DELETED_TOPIC, id.toString());
            } catch (Exception e) {
                System.err.println("Kafka indisponible, suppression effectuée en base uniquement.");
            }
        }
    }

    public void updateAvailability(Long id, boolean available) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        room.setAvailable(available);
        roomRepository.save(room);
    }

    /**
     * Kafka Consumer pour gérer les événements liés aux réservations.
     * Si une réservation est terminée ou annulée, la salle redevient disponible.
     * Si une réservation est créée, la salle devient indisponible.
     */
    @KafkaListener(topics = "reservation-events-topic", groupId = "room-service-group")
    public void handleReservationEvent(String message) {
        // Format attendu: "ACTION:ROOM_ID" (ex: "RESERVATION_CREATED:1" ou "RESERVATION_COMPLETED:1")
        String[] parts = message.split(":");
        if (parts.length == 2) {
            String action = parts[0];
            Long roomId = Long.parseLong(parts[1]);

            if ("RESERVATION_CREATED".equals(action)) {
                updateAvailability(roomId, false);
            } else if ("RESERVATION_COMPLETED".equals(action) || "RESERVATION_CANCELLED".equals(action)) {
                updateAvailability(roomId, true);
            }
        }
    }
}
