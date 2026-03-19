package ynov.ydai.reservation_service.patterns;

import ynov.ydai.reservation_service.entities.Reservation;
import ynov.ydai.reservation_service.entities.ReservationStatus;

public class ConfirmedState implements ReservationState {
    @Override
    public void complete(Reservation reservation) {
        reservation.setStatus(ReservationStatus.COMPLETED);
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
    }
}
