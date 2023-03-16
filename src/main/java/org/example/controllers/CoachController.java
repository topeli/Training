package org.example.controllers;
import org.example.models.Coach;
import org.example.services.CoachService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ComponentScan("org.example.controllers")
@RestController
@RequestMapping("/telegram")
public class CoachController {
    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/add/coach")
    public void addCoach(@RequestBody Coach coach){
        coachService.addCoach(coach);
    }

    @GetMapping("/get/coaches")
    public List<Coach> getAllCoaches(){
        return coachService.getAllCoaches();
    }
}

