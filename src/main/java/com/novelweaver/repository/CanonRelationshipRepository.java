package com.novelweaver.repository;

import com.novelweaver.model.CanonRelationship;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CanonRelationshipRepository extends JpaRepository<CanonRelationship, UUID> {
    List<CanonRelationship> findByProject(Project project);
}
