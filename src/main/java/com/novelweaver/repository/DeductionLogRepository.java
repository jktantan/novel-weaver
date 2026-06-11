package com.novelweaver.repository;

import com.novelweaver.model.Chapter;
import com.novelweaver.model.DeductionLog;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeductionLogRepository extends JpaRepository<DeductionLog, UUID> {
    List<DeductionLog> findByProjectAndChapter(Project project, Chapter chapter);

    List<DeductionLog> findByProjectAndType(Project project, String type);
}
