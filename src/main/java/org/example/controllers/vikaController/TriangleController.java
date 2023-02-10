package org.example.controllers.vikaController;

import org.example.models.Triangle;
import org.example.triangleService.Triangleservice;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@ComponentScan("com.example.demo.controllers")
@RestController
@RequestMapping("/telegram")
public class TriangleController {
    private final Triangleservice triangleservice;

    public TriangleController(Triangleservice triangleservice) {
        this.triangleservice = triangleservice;
    }

    @PostMapping("/add/triangle")
    public int Triangle(@RequestBody Triangle triangle) {

        return triangleservice.area(triangle);
    }
}

