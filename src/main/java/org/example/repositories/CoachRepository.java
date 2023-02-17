package org.example.repositories;
import org.example.models.Coach;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
public interface CoachRepository extends JpaRepository<Coach, Long>{
}
