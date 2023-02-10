package org.example.repositories.trianglerepository;

import org.example.models.Triangle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriangleRepository extends JpaRepository<Triangle, Long> {
}
