package com.novelweaver.repository;

import com.novelweaver.model.CanonEvent;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CanonEventRepository extends JpaRepository<CanonEvent, UUID> {
    List<CanonEvent> findByProject(Project project);

    @Query(value = "SELECT ce.* FROM canon_events ce WHERE ce.project_id = :pid ORDER BY ce.embedding <-> :vec::vector LIMIT :k", nativeQuery = true)
    List<CanonEvent> findSimilar(@Param("pid") UUID projectId, @Param("vec") String vector, @Param("k") int k);
}
