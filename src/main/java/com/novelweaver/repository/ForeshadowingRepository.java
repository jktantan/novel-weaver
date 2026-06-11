package com.novelweaver.repository;

import com.novelweaver.model.Foreshadowing;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForeshadowingRepository extends JpaRepository<Foreshadowing, UUID> {
    @Query("SELECT f FROM Foreshadowing f WHERE f.project = :proj AND f.status IN ('active', '🌱', '🌿') ORDER BY f.plantedChapter")
    List<Foreshadowing> findActiveByProject(@Param("proj") Project project);

    Optional<Foreshadowing> findByProjectAndCode(Project project, String code);
}
