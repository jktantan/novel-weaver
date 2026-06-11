package com.novelweaver.repository;

import com.novelweaver.model.CanonEvent;
import com.novelweaver.model.CanonEventStatus;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CanonEventStatusRepository extends JpaRepository<CanonEventStatus, UUID> {
    List<CanonEventStatus> findByProject(Project project);

    Optional<CanonEventStatus> findByProjectAndCanonEvent(Project project, CanonEvent canonEvent);
}
