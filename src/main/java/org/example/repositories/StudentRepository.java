package org.example.repositories;

import org.example.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query(value = "select * from students1533 where name = :name", nativeQuery = true)
    List<Student> findByName(String name);
    @Query(value = "select * from students1533 where class_group = :classGroup",nativeQuery = true)
    List<Student> findByGroup(String classGroup);
    @Query(value = "select distinct class_group from students1533",nativeQuery = true)
    List<String> findDifferentGroups();
    @Query(value = "select * from students1533 where chat_id=:chatId", nativeQuery = true)
    List<Student> studentByChatId(Long chatId);
}

