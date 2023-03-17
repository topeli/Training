package org.example.repositories;

import org.example.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query(value = "select * from students1533 where name = :name", nativeQuery = true)
    List<Student> findByName(String name);

}

