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

    @PostMapping("/add/{studentId}{coachId}/mark")
    public void addMark(@PathVariable(value = "studentId")Long studentId, @PathVariable(value = "coachId") Long coachId,
                        @RequestBody Mark mark)
            throws Exception {
        markService.addMark(mark, studentId, coachId);
    }
}
