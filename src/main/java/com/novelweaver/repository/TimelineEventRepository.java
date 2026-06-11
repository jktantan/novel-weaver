package com.novelweaver.repository;

import com.novelweaver.model.Project;
import com.novelweaver.model.Timeline;
import com.novelweaver.model.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {
    List<TimelineEvent> findByProjectAndTimelineOrderByAbsoluteOrder(Project project, Timeline timeline);

    List<TimelineEvent> findByProject(Project project);
}
