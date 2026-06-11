package com.novelweaver.repository;

import com.novelweaver.model.Project;
import com.novelweaver.model.Universe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniverseRepository extends JpaRepository<Universe, UUID> {
    List<Universe> findByProject(Project project);

    Optional<Universe> findByProjectAndName(Project project, String name);
}
