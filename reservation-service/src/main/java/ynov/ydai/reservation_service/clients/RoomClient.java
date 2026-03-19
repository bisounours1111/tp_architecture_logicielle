package ynov.ydai.reservation_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ynov.ydai.reservation_service.dto.RoomDTO;

@FeignClient(name = "room-service")
public interface RoomClient {
    @GetMapping("/api/rooms/{id}")
    RoomDTO getRoomById(@PathVariable("id") Long id);
}
