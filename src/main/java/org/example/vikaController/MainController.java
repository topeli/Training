package org.example.vikaController;

import org.example.models.vikaHW;
import org.example.triangleService.Triangleservice;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@ComponentScan("com.example.demo.controllers")
@RestController
@RequestMapping
public class MainController {
    private final Triangleservice triangleservice;

    public MainController(Triangleservice triangleservice) {
        this.triangleservice = triangleservice;
    }

    @PostMapping
    public int Triangle(@RequestBody vikaHW triangle) {

        return triangleservice.area(triangle);
    }
}

