package org.example.controllers;

import org.example.models.Student;
import org.example.models.Training;
import org.example.services.StudentService;
import org.example.services.TrainingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/telegram")
public class MainController {
    private final StudentService studentService;
    private final TrainingService trainingService;

    public MainController(StudentService studentService, TrainingService trainingService) {
        this.studentService = studentService;
        this.trainingService = trainingService;
    }

    @PostMapping("/add/student")
    public void addStudent(@RequestBody Student student) { // RequestBody можно добавить потом (чтобы показать, что создастся null)
        studentService.addStudent(student);
    }

    @PostMapping("/add/training")
    public void addTraining(@RequestBody Training training) {
        trainingService.addTraining(training);
    }
}
