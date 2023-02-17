package org.example.controllers;
import org.example.models.Training;
import org.example.models.Training;
import org.example.services.TrainingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/telegram")
public class MainTrainingController {
    private final TrainingService trainingService;

    public MainTrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }


    @PostMapping("/add/training")
    public void addTraining(@RequestBody Training training) { // RequestBody можно добавить потом (чтобы показать, что создастся null)
        trainingService.addTraining(training);
    }
}
