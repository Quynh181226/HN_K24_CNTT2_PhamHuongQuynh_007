package vn.rikkei.exam.restaurantreservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.restaurantreservation.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
}