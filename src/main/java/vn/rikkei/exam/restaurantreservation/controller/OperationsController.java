package vn.rikkei.exam.restaurantreservation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.restaurantreservation.dto.OperationsRequest;
import vn.rikkei.exam.restaurantreservation.dto.OperationsResponse;
import vn.rikkei.exam.restaurantreservation.service.ReservationService;

@Slf4j
@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final ReservationService reservationService;

    @PostMapping("/approve-request")
    public ResponseEntity<OperationsResponse> approveRequest(@RequestBody OperationsRequest request) {
        log.info("[Operations] requestId={} decision={}", request.getRequestId(), request.getDecision());
        try {
            String result = reservationService.approveOrReject(
                    request.getRequestId(),
                    request.getDecision(),
                    request.getNote());
            return ResponseEntity.ok(new OperationsResponse(true, result));
        } catch (IllegalArgumentException e) {
            log.warn("[Operations] Lỗi nghiệp vụ: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new OperationsResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("[Operations] Lỗi hệ thống: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new OperationsResponse(false, "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
