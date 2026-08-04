package com.jiaozhilong.gisagent.solution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.user.UserEntity;
import com.jiaozhilong.gisagent.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SolutionGenerationService {
    private final SolutionRunRepository repository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public SolutionGenerationService(SolutionRunRepository repository, UserRepository userRepository,
                                     ApplicationEventPublisher publisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SolutionDtos.GenerationRunResponse start(String projectId, String username, SolutionDtos.GenerationRequest request) {
        UserEntity requester = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户不存在"));
        SolutionRunEntity run = new SolutionRunEntity();
        run.setProjectId(projectId);
        run.setRequestedBy(requester);
        run.setStatus(SolutionEnums.TaskStatus.PENDING);
        run.setStage(SolutionEnums.Stage.PLANNING);
        run.setGroundingPolicy(request.groundingPolicy());
        run.setAllowModelSupplement(request.allowModelSupplement());
        run.setKnowledgeBaseIds(json(request.knowledgeBaseIds()));
        run.setOutputFormats(json(request.outputFormats()));
        run.setRagflowAssistantId(request.ragflowAssistantId());
        repository.save(run);
        publisher.publishEvent(new SolutionGenerationRequested(run.getId(), projectId, requester.getId(), request.ragflowAssistantId(),
                request.knowledgeBaseIds(), request.groundingPolicy(), request.allowModelSupplement()));
        return response(run);
    }

    @Transactional(readOnly = true)
    public SolutionDtos.GenerationRunResponse get(UUID id) {
        return response(repository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "方案生成任务不存在")));
    }

    SolutionDtos.GenerationRunResponse response(SolutionRunEntity run) {
        var sections = run.getSections().stream().distinct().map(section -> new SolutionDtos.SectionResponse(
                section.getId().toString(), section.getTitle(), section.getContent(), section.getSourceType(),
                section.getEvidenceCoverage().doubleValue(), section.getCitations().stream()
                .map(citation -> citation.getRagflowChunkId() != null ? citation.getRagflowChunkId() : citation.getId().toString()).toList(),
                section.getConfirmationReason())).toList();
        return new SolutionDtos.GenerationRunResponse(run.getId(), run.getProjectId(), run.getStatus(), run.getStage(),
                run.getRagflowSessionId(), run.getModelName(), run.getEvidenceCoverage().doubleValue(), sections,
                run.getCreatedAt(), run.getUpdatedAt(), run.getErrorMessage());
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("无法序列化生成参数", exception); }
    }
}
