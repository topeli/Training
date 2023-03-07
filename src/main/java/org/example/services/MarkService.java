package org.example.services;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.repositories.CoachRepository;
import org.example.repositories.MarkRepository;
import org.example.repositories.StudentRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.util.Optional;

@ComponentScan("org.example.services")
@Service
public class MarkService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;


    public MarkService(MarkRepository markRepository, StudentRepository studentRepository, CoachRepository coachRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.coachRepository = coachRepository;
    }

    public void addMark(Mark mark, Long studentId, Long coachId) throws Exception {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("студент не найден"));
        mark.setStudent(student);
        Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new Exception("тренер не найден"));;
        mark.setCoach(coach);
        markRepository.save(mark);
    }
}
