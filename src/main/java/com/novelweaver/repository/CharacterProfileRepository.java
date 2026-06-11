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
    Optional<CharacterProfile> findByProjectAndName(Project project, String name);

    List<CharacterProfile> findAllByProject(Project project);

    List<CharacterProfile> findByProjectAndNameIn(Project project, List<String> names);
}
