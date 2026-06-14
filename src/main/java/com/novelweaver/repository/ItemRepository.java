package com.novelweaver.repository;

import com.novelweaver.model.Item;
import com.novelweaver.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
    /**
     * Find all items by project and name (may return multiple if identity differs).
     */
    List<Item> findByProjectAndName(Project project, String name);

    /**
     * Exact match by project, name, and identity JSON string.
     */
    Optional<Item> findByProjectAndNameAndIdentity(Project project, String name, String identity);

    List<Item> findAllByProject(Project project);
}
