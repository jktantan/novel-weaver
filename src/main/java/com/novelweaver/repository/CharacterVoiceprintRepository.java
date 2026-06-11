package com.novelweaver.repository;

import com.novelweaver.model.CharacterProfile;
import com.novelweaver.model.CharacterVoiceprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterVoiceprintRepository extends JpaRepository<CharacterVoiceprint, UUID> {
    List<CharacterVoiceprint> findByCharacter(CharacterProfile character);

    @Query(value = "SELECT cv.* FROM character_voiceprints cv WHERE cv.character_id = :cid ORDER BY cv.embedding <-> :vec::vector LIMIT :k", nativeQuery = true)
    List<CharacterVoiceprint> findSimilar(@Param("cid") UUID characterId, @Param("vec") String vector, @Param("k") int k);
}
