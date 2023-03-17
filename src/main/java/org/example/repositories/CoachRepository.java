package org.example.repositories;
import org.example.models.Coach;
import org.example.models.Student;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long>{
    @Query(value = "select * from coaches where name = :name",nativeQuery = true)
    List<Coach> findByName(String name);
}
