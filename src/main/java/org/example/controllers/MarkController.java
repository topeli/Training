package org.example.controllers;
import org.example.models.Mark;
import org.example.services.MarkService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;
@ComponentScan("org.example.controllers")
@RestController
@RequestMapping("/telegram")
public class MarkController {
    private final MarkService markService;


    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @PostMapping("/add/mark")
    public void addCoach(@RequestBody Mark mark){
        markService.addMark(mark);
    }
}
