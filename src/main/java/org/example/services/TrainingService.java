package org.example.services;
import org.example.models.Coach;
import org.example.models.Student;
import org.example.models.Training;
import org.example.repositories.CoachRepository;
import org.example.repositories.StudentRepository;
import org.example.repositories.TrainingRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
@ComponentScan("org.example.services")
@Service
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;

    public TrainingService(TrainingRepository trainingRepository, StudentRepository studentRepository, CoachRepository coachRepository) {

        this.trainingRepository = trainingRepository;
        this.studentRepository = studentRepository;
        this.coachRepository = coachRepository;
    }

    public void addTraining(Training training, Long studentId) throws Exception {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("студент не найден"));
        training.setStudent(student);
        trainingRepository.save(training);
    }
    public void addTrainingCoach(Training training, Long coachId) throws Exception{
        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new Exception("тренер не найден"));
        training.setCoach(coach);
        trainingRepository.save(training);
    }
}
