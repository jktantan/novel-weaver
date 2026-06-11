package com.novelweaver.repository;

import com.novelweaver.model.CanonSource;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CanonSourceRepository extends JpaRepository<CanonSource, UUID> {
    List<CanonSource> findByProjectAndName(Project project, String name);
}
