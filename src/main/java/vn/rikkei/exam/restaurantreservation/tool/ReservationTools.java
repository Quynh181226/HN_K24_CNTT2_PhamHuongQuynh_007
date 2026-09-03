package vn.rikkei.exam.restaurantreservation.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.rikkei.exam.restaurantreservation.service.ReservationService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReservationTools {

    private final ReservationService reservationService;

    @Tool(description = "Kiểm tra tình trạng bàn nhà hàng còn khả dụng theo khoảng ngày. resourceType: STD hoặc PRM")
    public String getTableAvailability(
            @ToolParam(description = "Mã loại bàn: STD hoặc PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu yyyy-MM-dd") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc yyyy-MM-dd") LocalDate endDate) {
        return reservationService.getTableAvailability(resourceType, startDate, endDate);
    }

    @Tool(description = "Tạo yêu cầu đặt bàn PENDING. Trả về requestId và thông tin tóm tắt")
    public String createTableReservationRequest(
            @ToolParam(description = "ID người dùng ví dụ USR-001") String userId,
            @ToolParam(description = "Mã loại bàn STD hoặc PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu yyyy-MM-dd") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc yyyy-MM-dd") LocalDate endDate,
            @ToolParam(description = "Số người tham gia") Integer participantCount,
            @ToolParam(description = "Mục đích 10-200 ký tự") String purpose) {
        return reservationService.createTableReservationRequest(
                userId, resourceType, startDate, endDate, participantCount, purpose);
    }
}