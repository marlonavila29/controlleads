package com.controlleads.catalogs;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
