package ynov.ydai.room_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ynov.ydai.room_service.entities.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
}
