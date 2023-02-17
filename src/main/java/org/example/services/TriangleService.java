package org.example.services;

import org.example.models.Triangle;
import org.example.repositories.TriangleRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
@ComponentScan("org.example.triangleService")
@Service
public class TriangleService {
        private final TriangleRepository triangleRepository;

    public TriangleService(TriangleRepository triangleRepository) {
        this.triangleRepository = triangleRepository;
    }
        public int area(Triangle triangle) {
            triangleRepository.save(triangle);
            return triangle.getA() * triangle.getB() / 2;

        }
}

