package vn.rikkei.exam.restaurantreservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.restaurantreservation.model.ResourceType;

public interface ResourceTypeRepository extends JpaRepository<ResourceType, String> {
}