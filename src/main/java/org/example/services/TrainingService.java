package org.example.services;
import org.example.models.Training;
import org.example.repositories.TrainingRepository;
import org.springframework.stereotype.Service;
@Service
public class TrainingService {
    private final TrainingRepository trainingRepository;

    public TrainingService(TrainingRepository trainingRepository, TrainingRepository trainingRepository1) {
        this.trainingRepository = trainingRepository1;
    }

    public void addTraining(Training training) {
        trainingRepository.save(training);
    }
}
