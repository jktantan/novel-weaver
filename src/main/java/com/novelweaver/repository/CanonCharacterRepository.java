package com.novelweaver.repository;

import com.novelweaver.model.CanonCharacter;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CanonCharacterRepository extends JpaRepository<CanonCharacter, UUID> {
    List<CanonCharacter> findByProject(Project project);
}
