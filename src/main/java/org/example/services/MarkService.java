package org.example.services;
import org.example.models.Mark;
import org.example.models.Student;
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


    public MarkService(MarkRepository markRepository, StudentRepository studentRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
    }

    public void addMark(Mark mark, Long studentId) throws Exception {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("студент не найден"));
        mark.setStudent(student);
        markRepository.save(mark);
    }
}
