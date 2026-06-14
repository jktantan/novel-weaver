package com.novelweaver.repository;

import com.novelweaver.model.Location;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    /**
     * Find all locations by project and name (may return multiple if identity differs).
     */
    List<Location> findByProjectAndName(Project project, String name);

    /**
     * Exact match by project, name, and identity JSON string.
     */
    Optional<Location> findByProjectAndNameAndIdentity(Project project, String name, String identity);

    List<Location> findAllByProject(Project project);
}
