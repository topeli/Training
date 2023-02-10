package org.example.controllers;

import org.example.models.Rectangle;
import org.example.services.RectangleService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ComponentScan("com.example.demo.controllers")


@RestController
@RequestMapping("/telegram")
public class RectangleController
{
    private final RectangleService rectangleService;
    public RectangleController(RectangleService rectangleService) {
        this.rectangleService = rectangleService;
    }
    @PostMapping("/rectangle")
    public int Rectangle(@RequestBody Rectangle rec) {return rectangleService.rectangle(rec);}
}
