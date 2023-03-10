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
    @PostMapping("/add/{studentId}/{coachId}/training")
    public void addTraining(@PathVariable(value = "studentId") Long studentId, @PathVariable(value = "coachId") Long coachId,
                            @RequestBody Training training) throws Exception {
        trainingService.addTraining(training, studentId, coachId);
    }


}
