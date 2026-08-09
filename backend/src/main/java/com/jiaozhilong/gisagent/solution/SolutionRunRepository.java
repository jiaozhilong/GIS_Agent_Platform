package com.jiaozhilong.gisagent.solution;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface SolutionRunRepository extends JpaRepository<SolutionRunEntity, UUID> {
    @EntityGraph(attributePaths = {"sections", "sections.citations"})
    Optional<SolutionRunEntity> findDetailedById(UUID id);
    List<SolutionRunEntity> findAllByOrderByCreatedAtDesc();
    List<SolutionRunEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
