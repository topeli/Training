package org.example.triangleService;

import org.example.models.Triangle;
import org.example.repositories.trianglerepository.TriangleRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

@ComponentScan("org.example.triangleService")
@Service
public class Triangleservice {
    private final TriangleRepository triangleRepository;
    public Triangleservice(TriangleRepository triangleRepository){
        this.triangleRepository = triangleRepository;
    }


    public int area(Triangle triangle) {
        triangleRepository.save(triangle);
        return triangle.getA() * triangle.getB() / 2;

    }
}
