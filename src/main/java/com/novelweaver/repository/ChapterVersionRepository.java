package com.novelweaver.repository;

import com.novelweaver.model.Chapter;
import com.novelweaver.model.ChapterVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, UUID> {
    Optional<ChapterVersion> findByChapterAndVersion(Chapter chapter, Integer version);

    @Query("SELECT COALESCE(MAX(cv.version), 0) FROM ChapterVersion cv WHERE cv.chapter = :chapter")
    Integer findMaxVersionByChapter(@Param("chapter") Chapter chapter);
}
