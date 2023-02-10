package org.example.triangleService;

import org.example.models.vikaHW;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

@ComponentScan("org.example.triangleService")
@Service
public class Triangleservice {
    private final triangleRepository triangleRepository;
    public Triangleservice(triangleRepository triangleRepository){
        this.triangleRepository = triangleRepository;
    }


    public int area(vikaHW triangle) {
        return triangle.a * triangle.b /2;
    }
}
