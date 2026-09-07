package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Consulta A - Paso 34
    Optional<Animal> findByAnimalCode(String animalCode);

    // Consulta B - Paso 34
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    // Paso 35 - Animal -> rescueCase -> status
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Paso 36 - Animal -> rescueCase -> rescueCenter -> code
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);
}
