package org.example.repositories;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface MarkRepository extends JpaRepository<Mark, Long>{
    @Query(value = "select mark from marks join students1533 on students1533.id = marks.student_id where students1533.name = :name", nativeQuery = true)
    List<Mark> getStudentMarksByName(String name);

    @Query(value = "select * from marks where student_id = :id", nativeQuery = true)
    List<Mark> getMarksByStudentId(Long id);
}
