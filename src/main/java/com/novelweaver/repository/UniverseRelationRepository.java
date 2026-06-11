package com.novelweaver.repository;

import com.novelweaver.model.Project;
import com.novelweaver.model.UniverseRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UniverseRelationRepository extends JpaRepository<UniverseRelation, UUID> {
    List<UniverseRelation> findByProject(Project project);

    List<UniverseRelation> findByFromUniverseId(UUID universeId);

    List<UniverseRelation> findByToUniverseId(UUID universeId);
}
