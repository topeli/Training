package org.example.controllers;

import org.example.models.Training;
import org.example.models.Training;
import org.example.services.TrainingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telegram")
public class MainTrainingController {
    private final TrainingService trainingService;

    public MainTrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }


    @PostMapping("/add/{coachId}/training")
    public void addTrainingCoach(@PathVariable(value = "coachId") Long coachId,
                            @RequestBody Training training) throws Exception {
        trainingService.addTrainingCoach(training, coachId);
    }

    @PostMapping("/add/{studentId}/training")
    public void addTrainingStudent(@PathVariable(value = "studentId") Long studentId,
                            @RequestBody Training training) throws Exception {
        trainingService.addTrainingStudent(training, studentId);
    }


}
