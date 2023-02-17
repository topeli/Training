package org.example.repositories;
import org.example.models.Coach;
import org.example.models.Mark;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
public interface MarkRepository extends JpaRepository<Mark, Long>{
}
