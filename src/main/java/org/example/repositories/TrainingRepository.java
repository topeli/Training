package org.example.repositories;
import org.example.models.Coach;
import org.example.models.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    @Query(value = "select *  from trainings where coach_id = :coachId", nativeQuery = true)
    List<Training> trainingByCoachId(Long coachId);
    @Query(value = "select *  from trainings where class_group = :classGroup", nativeQuery = true)
    List<Training> trainingByStudent(String classGroup);
}
