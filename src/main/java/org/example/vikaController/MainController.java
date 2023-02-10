package org.example.vikaController;

import org.example.models.Triangle;
//import org.example.models.vikaHW;
import org.example.triangleService.Triangleservice;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;

@ComponentScan("com.example.demo.controllers")
@RestController
@RequestMapping("/telegram")
public class MainController {
    private final Triangleservice triangleservice;

    public MainController(Triangleservice triangleservice) {
        this.triangleservice = triangleservice;
    }

    @PostMapping("/add/triangle")
    public int Triangle(@RequestBody Triangle triangle) {

        return triangleservice.area(triangle);
    }
}

