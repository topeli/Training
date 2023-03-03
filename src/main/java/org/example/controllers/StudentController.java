package org.example.controllers;

import org.example.models.Student;
import org.example.services.StudentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/telegram")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/add/student")
    public void addStudent(@RequestBody Student student) { // RequestBody можно добавить потом (чтобы показать, что создастся null)
        studentService.addStudent(student);
    }
}
