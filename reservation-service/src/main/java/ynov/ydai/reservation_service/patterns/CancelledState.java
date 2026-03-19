package ynov.ydai.reservation_service.patterns;

import ynov.ydai.reservation_service.entities.Reservation;

public class CancelledState implements ReservationState {
    @Override
    public void complete(Reservation reservation) {
        throw new IllegalStateException("Cannot complete a cancelled reservation");
    }

    @Override
    public void cancel(Reservation reservation) {
        throw new IllegalStateException("Reservation is already cancelled");
    }
}
