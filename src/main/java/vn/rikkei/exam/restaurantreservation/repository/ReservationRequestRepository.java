package vn.rikkei.exam.restaurantreservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.restaurantreservation.model.ReservationRequest;
import vn.rikkei.exam.restaurantreservation.model.ReservationStatus;

import java.util.Optional;

public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, String> {
    Optional<ReservationRequest> findByRequestIdAndStatus(String requestId, ReservationStatus status);
}