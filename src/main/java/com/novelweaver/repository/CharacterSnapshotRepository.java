package com.novelweaver.repository;

import com.novelweaver.model.Chapter;
import com.novelweaver.model.CharacterSnapshot;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterSnapshotRepository extends JpaRepository<CharacterSnapshot, UUID> {
    @Query("SELECT cs FROM CharacterSnapshot cs JOIN FETCH cs.character c WHERE c.project = :proj AND cs.chapter.chapterNumber <= :ch ORDER BY c.name, cs.chapter.chapterNumber DESC")
    List<CharacterSnapshot> findLatestForProject(@Param("proj") Project project, @Param("ch") Integer chapterNumber);

    Optional<CharacterSnapshot> findByChapterAndCharacterName(Chapter chapter, String characterName);

    @Query("SELECT cs FROM CharacterSnapshot cs JOIN FETCH cs.chapter ch WHERE cs.project = :proj AND cs.characterName IN :names ORDER BY ch.chapterNumber DESC")
    List<CharacterSnapshot> findByProjectAndCharacterNameIn(@Param("proj") Project project, @Param("names") List<String> names);

    @Query("SELECT DISTINCT cs.characterName FROM CharacterSnapshot cs WHERE cs.project = :proj AND cs.chapter.chapterNumber = :ch")
    List<String> findCharacterNamesInChapter(@Param("proj") Project project, @Param("ch") int chapterNumber);
}
