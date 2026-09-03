package vn.rikkei.exam.restaurantreservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rikkei.exam.restaurantreservation.model.*;
import vn.rikkei.exam.restaurantreservation.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final AppUserRepository userRepo;
    private final ResourceTypeRepository typeRepo;
    private final ResourceInventoryRepository inventoryRepo;
    private final ReservationRequestRepository requestRepo;

    public String getTableAvailability(String resourceType, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            return "Lỗi: startDate phải nhỏ hơn endDate";
        }

        ResourceType type = typeRepo.findById(resourceType).orElse(null);
        if (type == null || Boolean.FALSE.equals(type.getActive())) {
            return "Không tìm thấy loại bàn hợp lệ: " + resourceType;
        }

        List<ResourceInventory> inventories = inventoryRepo.findAvailability(resourceType, startDate, endDate);
        if (inventories.isEmpty()) {
            return "Không có dữ liệu khả dụng cho " + resourceType + " từ " + startDate + " đến " + endDate;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Khả dụng ").append(type.getDisplayName()).append(" (").append(resourceType).append("):\n");
        for (ResourceInventory inv : inventories) {
            sb.append("- ").append(inv.getAvailableDate())
                    .append(": ").append(inv.getAvailableSlots()).append(" slot\n");
        }
        return sb.toString();
    }

    @Transactional
    public String createTableReservationRequest(String userId, String resourceType,
                                                LocalDate startDate, LocalDate endDate,
                                                Integer participantCount, String purpose) {

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));

        ResourceType type = typeRepo.findById(resourceType)
                .orElseThrow(() -> new IllegalArgumentException("Loại bàn không tồn tại: " + resourceType));

        if (Boolean.FALSE.equals(type.getActive())) {
            throw new IllegalArgumentException("Loại bàn không còn hoạt động");
        }

        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("startDate phải nhỏ hơn endDate");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > 14) {
            throw new IllegalArgumentException("Một yêu cầu tối đa 14 ngày");
        }

        if (participantCount == null || participantCount < 1) {
            throw new IllegalArgumentException("Số người phải >= 1");
        }

        if (participantCount > type.getMaxParticipants()) {
            throw new IllegalArgumentException("Vượt sức chứa tối đa");
        }

        // PREMIUM tối thiểu 2 người
        if ("PRM".equalsIgnoreCase(resourceType) && participantCount < 2) {
            throw new IllegalArgumentException("PREMIUM chỉ dành cho từ 2 người trở lên");
        }

        if (purpose == null || purpose.trim().length() < 10 || purpose.trim().length() > 200) {
            throw new IllegalArgumentException("Mục đích phải từ 10 đến 200 ký tự");
        }

        List<ResourceInventory> inventories = inventoryRepo.findAvailability(resourceType, startDate, endDate);
        long expectedDays = ChronoUnit.DAYS.between(startDate, endDate);

        if (inventories.size() < expectedDays) {
            throw new IllegalArgumentException("Không đủ dữ liệu khả dụng trong khoảng ngày");
        }

        for (ResourceInventory inv : inventories) {
            if (inv.getAvailableSlots() == null || inv.getAvailableSlots() < 1) {
                throw new IllegalArgumentException("Không còn slot vào ngày " + inv.getAvailableDate());
            }
        }

        String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ReservationRequest request = ReservationRequest.builder()
                .requestId(requestId)
                .requester(user)
                .resourceType(type)
                .startDate(startDate)
                .endDate(endDate)
                .participantCount(participantCount)
                .purpose(purpose.trim())
                .status(ReservationStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        requestRepo.save(request);

        return "Tạo yêu cầu thành công. requestId=" + requestId
                + " - status=PENDING"
                + " - loại bàn=" + type.getDisplayName()
                + " - từ " + startDate + " đến " + endDate
                + " - số người=" + participantCount
                + " - mục đích=" + purpose.trim();
    }

    @Transactional
    public String approveOrReject(String requestId, String decision, String note) {
        ReservationRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy requestId: " + requestId));

        if (req.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ xử lý được yêu cầu PENDING. Hiện tại: " + req.getStatus());
        }

        if (!"APPROVE".equalsIgnoreCase(decision) && !"REJECT".equalsIgnoreCase(decision)) {
            throw new IllegalArgumentException("decision phải là APPROVE hoặc REJECT");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {
            List<ResourceInventory> inventories = inventoryRepo.findAvailability(
                    req.getResourceType().getResourceCode(),
                    req.getStartDate(),
                    req.getEndDate());

            for (ResourceInventory inv : inventories) {
                if (inv.getAvailableSlots() == null || inv.getAvailableSlots() < 1) {
                    throw new IllegalArgumentException("Không còn slot vào ngày " + inv.getAvailableDate() + ". Không thể APPROVE");
                }
            }

            // trừ slot
            for (ResourceInventory inv : inventories) {
                inv.setAvailableSlots(inv.getAvailableSlots() - 1);
                inventoryRepo.save(inv);
            }
            req.setStatus(ReservationStatus.APPROVED);
        } else {
            req.setStatus(ReservationStatus.REJECTED);
        }

        req.setDecisionNote(note);
        req.setUpdatedAt(Instant.now());
        requestRepo.save(req);

        return "Đã " + decision.toUpperCase() + " request " + requestId
                + (note != null ? " - note: " + note : "");
    }
}