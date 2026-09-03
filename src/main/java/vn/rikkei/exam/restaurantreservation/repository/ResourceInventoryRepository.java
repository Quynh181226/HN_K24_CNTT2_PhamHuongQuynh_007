package vn.rikkei.exam.restaurantreservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.rikkei.exam.restaurantreservation.model.ResourceInventory;

import java.time.LocalDate;
import java.util.List;

public interface ResourceInventoryRepository extends JpaRepository<ResourceInventory, Long> {

    @Query("""
        SELECT ri FROM ResourceInventory ri
        WHERE ri.resourceType.resourceCode = :resourceCode
          AND ri.availableDate >= :startDate
          AND ri.availableDate < :endDate
        ORDER BY ri.availableDate
        """)
    List<ResourceInventory> findAvailability(
            @Param("resourceCode") String resourceCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}