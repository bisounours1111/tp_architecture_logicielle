package ynov.ydai.reservation_service.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.services.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Management", description = "Endpoints for managing coworking reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Get all reservations")
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @PostMapping
    @Operation(summary = "Create a new reservation (checks room availability and member status via REST)")
    public ResponseEntity<?> createReservation(@RequestBody Reservation reservation) {
        try {
            return ResponseEntity.ok(reservationService.createReservation(reservation));
        } catch (Exception e) {
            e.printStackTrace(); // Pour voir l'erreur réelle dans la console
            return ResponseEntity.status(500).body("Erreur lors de la réservation : " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation (uses State Pattern)")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reservationService.cancelReservation(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a reservation (uses State Pattern)")
    public ResponseEntity<Reservation> completeReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reservationService.completeReservation(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
