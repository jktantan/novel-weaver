package com.novelweaver.repository;

import com.novelweaver.model.Project;
import com.novelweaver.model.TimelineLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineLinkRepository extends JpaRepository<TimelineLink, UUID> {
    List<TimelineLink> findByProject(Project project);

    List<TimelineLink> findByFromTimelineId(UUID timelineId);

    List<TimelineLink> findByToTimelineId(UUID timelineId);
}
