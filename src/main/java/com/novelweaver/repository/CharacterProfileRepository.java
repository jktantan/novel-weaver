package com.novelweaver.repository;

import com.novelweaver.model.CharacterProfile;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, UUID> {
    /**
     * Find all profiles by project and name (may return multiple if identity differs).
     */
    List<CharacterProfile> findByProjectAndName(Project project, String name);

    /**
     * Exact match by project, name, and identity JSON string.
     */
    Optional<CharacterProfile> findByProjectAndNameAndIdentity(Project project, String name, String identity);

    List<CharacterProfile> findAllByProject(Project project);

    List<CharacterProfile> findByProjectAndNameIn(Project project, List<String> names);
}
