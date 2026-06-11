package com.novelweaver.repository;

import com.novelweaver.model.CharacterRelationship;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterRelationshipRepository extends JpaRepository<CharacterRelationship, UUID> {
    List<CharacterRelationship> findByProject(Project project);

    List<CharacterRelationship> findByProjectAndFromChar(Project project, String fromChar);

    List<CharacterRelationship> findByProjectAndToChar(Project project, String toChar);

    List<CharacterRelationship> findByProjectAndFromCharIn(Project project, List<String> fromChars);
}
