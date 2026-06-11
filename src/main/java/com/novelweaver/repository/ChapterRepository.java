package com.novelweaver.repository;

import com.novelweaver.model.Chapter;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    Optional<Chapter> findByProjectAndChapterNumber(Project project, Integer number);

    @Query("SELECT c FROM Chapter c WHERE c.project = :proj ORDER BY c.chapterNumber")
    List<Chapter> findAllByProject(@Param("proj") Project project);

    List<Chapter> findByProjectOrderByChapterNumber(Project project);
}
