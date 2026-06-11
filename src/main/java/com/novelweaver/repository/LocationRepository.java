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
    Optional<Location> findByProjectAndName(Project project, String name);

    List<Location> findAllByProject(Project project);
}
