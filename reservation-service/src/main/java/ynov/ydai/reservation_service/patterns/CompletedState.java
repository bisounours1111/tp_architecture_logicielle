package ynov.ydai.reservation_service.patterns;

import ynov.ydai.reservation_service.entities.Reservation;

public class CompletedState implements ReservationState {
    @Override
    public void complete(Reservation reservation) {
        throw new IllegalStateException("Reservation is already completed");
    }

    @Override
    public void cancel(Reservation reservation) {
        throw new IllegalStateException("Cannot cancel a completed reservation");
    }
}
