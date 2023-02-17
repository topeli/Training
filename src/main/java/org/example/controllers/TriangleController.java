package org.example.controllers;

import org.example.models.Triangle;
import org.example.services.TriangleService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@ComponentScan("com.example.demo.controllers")
@RestController
@RequestMapping("/telegram")
public class TriangleController {
    private final TriangleService triangleservice;

    public TriangleController(TriangleService triangleservice) {
        this.triangleservice = triangleservice;
    }

    @PostMapping("/add/triangle")
    public int Triangle(@RequestBody Triangle triangle) {

        return triangleservice.area(triangle);
    }
}

