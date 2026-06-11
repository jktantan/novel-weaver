package com.novelweaver.repository;

import com.novelweaver.model.Chapter;
import com.novelweaver.model.ChapterParagraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterParagraphRepository extends JpaRepository<ChapterParagraph, UUID> {
    List<ChapterParagraph> findByChapterOrderBySeq(Chapter chapter);

    @Query("SELECT cp FROM ChapterParagraph cp JOIN FETCH cp.chapter WHERE cp.id IN :ids")
    List<ChapterParagraph> findByIdInWithChapter(@Param("ids") List<UUID> ids);

    @Query(value = "SELECT cp.* FROM chapter_paragraphs cp WHERE cp.project_id = :pid ORDER BY cp.embedding <-> :vec::vector LIMIT :k", nativeQuery = true)
    List<ChapterParagraph> findSimilar(@Param("pid") UUID projectId, @Param("vec") String vector, @Param("k") int k);
}
