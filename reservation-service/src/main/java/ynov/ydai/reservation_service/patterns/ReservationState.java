package ynov.ydai.reservation_service.patterns;

import ynov.ydai.reservation_service.entities.Reservation;

public interface ReservationState {
    void complete(Reservation reservation);
    void cancel(Reservation reservation);
}
