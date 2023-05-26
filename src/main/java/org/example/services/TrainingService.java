package org.example.services;
import org.example.models.Coach;
import org.example.models.Training;
import org.example.repositories.CoachRepository;
import org.example.repositories.TrainingRepository;
import org.springframework.stereotype.Service;
@Service
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final CoachRepository coachRepository;

    public TrainingService(TrainingRepository trainingRepository, CoachRepository coachRepository) {
        this.trainingRepository = trainingRepository;
        this.coachRepository = coachRepository;
    }

    public void addTraining(Training training) {
        trainingRepository.save(training);
    }
}