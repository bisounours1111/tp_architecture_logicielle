package ynov.ydai.reservation_service.patterns;

import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.entities.ReservationStatus;

/**
 * State Pattern pour gérer le cycle de vie d'une réservation.
 * Justification : Ce pattern permet de centraliser la logique de transition
 * entre les états (CONFIRMED -> COMPLETED/CANCELLED) et d'ajouter facilement
 * des règles métier spécifiques à chaque état.
 */
public interface ReservationState {
    void complete(Reservation reservation);
    void cancel(Reservation reservation);
}
