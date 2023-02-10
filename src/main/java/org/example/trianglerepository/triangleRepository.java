package org.example.trianglerepository;

import org.example.models.Triangle;
//import org.example.models.vikaHW;
import org.springframework.data.jpa.repository.JpaRepository;

public interface triangleRepository extends JpaRepository<Triangle, Triangle> {
}
