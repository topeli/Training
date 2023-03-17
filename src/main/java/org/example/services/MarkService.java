package org.example.services;
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
public class MarkService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;


    public MarkService(MarkRepository markRepository, StudentRepository studentRepository, CoachRepository coachRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.coachRepository = coachRepository;
    }

    public void addMark(int mark, String coachName, String studentName) throws Exception {
        Mark mark1 = new Mark(mark);
        try
        {
            Student student = studentRepository.findByName(studentName).get(0);
            mark1.setStudent(student);
        }
        catch (Exception e){}
        try
        {
            Coach coach = coachRepository.findByName(coachName).get(0);
            mark1.setCoach(coach);
        }
        catch (Exception e){}
        markRepository.save(mark1);
    }

    public List<Mark> getAllMarks() {
        return markRepository.findAll();
    }
}
