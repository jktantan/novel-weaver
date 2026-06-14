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
    Optional<Item> findByProjectAndName(Project project, String name);

    List<Item> findAllByProject(Project project);
}
