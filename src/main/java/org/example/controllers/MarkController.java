package org.example.controllers;

import org.example.models.Mark;
import org.example.models.Student;
import org.example.services.MarkService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ComponentScan("org.example.controllers")
@RestController
@RequestMapping("/telegram")
public class MarkController {
    private final MarkService markService;


    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @GetMapping("/get/mark/{name}")
    public List<Mark> getStudentMarks(@PathVariable String name){
        return markService.getStudentMarks(name);
    }
}
