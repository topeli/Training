package org.example.services;

import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.repositories.CoachRepository;
import org.example.repositories.MarkRepository;
import org.example.repositories.StudentRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@ComponentScan("org.example.services")
@Service
@Slf4j
public class MarkService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;


    public MarkService(MarkRepository markRepository, StudentRepository studentRepository, CoachRepository coachRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.coachRepository = coachRepository;
    }

    public void addMark(int mark, Coach coach, Long studentId) throws Exception {
        Mark mark1 = new Mark(mark);
        try {
            Student student = studentRepository.findById(studentId).orElseThrow();
            mark1.setStudent(student);
        } catch (Exception e) {
        }

        mark1.setCoach(coach);

        markRepository.save(mark1);
    }

    public List<Mark> getStudentMarks(String name) {
        return markRepository.getStudentMarksByName(name);
    }
}
