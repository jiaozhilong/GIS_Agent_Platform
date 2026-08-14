package com.jiaozhilong.gisagent.solution;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SolutionSectionRepository extends JpaRepository<SolutionSectionEntity, UUID> {
    @EntityGraph(attributePaths = {"run", "run.requestedBy", "citations"})
    Optional<SolutionSectionEntity> findDetailedById(UUID id);
}
