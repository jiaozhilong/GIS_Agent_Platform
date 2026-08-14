package com.jiaozhilong.gisagent.solution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.user.UserEntity;
import com.jiaozhilong.gisagent.user.UserRepository;
import com.jiaozhilong.gisagent.project.ProjectService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class SolutionGenerationService {
    private final SolutionRunRepository repository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final SolutionSectionRepository sectionRepository;

    public SolutionGenerationService(SolutionRunRepository repository, UserRepository userRepository,
                                     ApplicationEventPublisher publisher, ObjectMapper objectMapper, ProjectService projectService,
                                     SolutionSectionRepository sectionRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.sectionRepository = sectionRepository;
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
        projectService.markGenerating(projectId);
        publisher.publishEvent(new SolutionGenerationRequested(run.getId(), projectId, requester.getId(), request.ragflowAssistantId(),
                request.knowledgeBaseIds(), request.groundingPolicy(), request.allowModelSupplement()));
        return response(run);
    }

    @Transactional(readOnly = true)
    public SolutionDtos.GenerationRunResponse get(UUID id) {
        return response(repository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "方案生成任务不存在")));
    }

    @Transactional(readOnly = true)
    public List<SolutionDtos.GenerationRunResponse> list(String projectId) {
        List<SolutionRunEntity> runs = projectId == null || projectId.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return runs.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SolutionDtos.SectionResponse getSection(UUID id) {
        return sectionResponse(requireSection(id));
    }

    @Transactional
    public SolutionDtos.SectionResponse updateSection(UUID id, SolutionDtos.UpdateSectionRequest request) {
        SolutionSectionEntity section = requireSection(id);
        if (section.isLocked()) throw new BusinessException(HttpStatus.CONFLICT, "SECTION_LOCKED", "章节已锁定，请先解锁");
        section.setTitle(request.title().trim());
        section.setContent(request.content());
        section.setSectionStatus("EDITED");
        return sectionResponse(sectionRepository.save(section));
    }

    @Transactional
    public SolutionDtos.SectionResponse lockSection(UUID id, boolean locked) {
        SolutionSectionEntity section = requireSection(id);
        section.setLocked(locked);
        return sectionResponse(sectionRepository.save(section));
    }

    @Transactional
    public SolutionDtos.SectionResponse regenerateSection(UUID id, SolutionDtos.RegenerateSectionRequest request) {
        SolutionSectionEntity section = requireSection(id);
        if (section.isLocked()) throw new BusinessException(HttpStatus.CONFLICT, "SECTION_LOCKED", "章节已锁定，不能重新生成");
        section.setSectionStatus("PENDING_REGENERATION");
        sectionRepository.save(section);
        publisher.publishEvent(new SolutionSectionRegenerationRequested(id,
                request.additionalInstruction() == null ? "" : request.additionalInstruction().trim()));
        return sectionResponse(section);
    }

    SolutionDtos.GenerationRunResponse response(SolutionRunEntity run) {
        var sections = run.getSections().stream().distinct().map(this::sectionResponse).toList();
        return new SolutionDtos.GenerationRunResponse(run.getId(), run.getProjectId(), run.getStatus(), run.getStage(),
                run.getRagflowSessionId(), run.getModelName(), run.getEvidenceCoverage().doubleValue(), sections,
                run.getCreatedAt(), run.getUpdatedAt(), run.getErrorMessage());
    }

    private SolutionDtos.SectionResponse sectionResponse(SolutionSectionEntity section) {
        return new SolutionDtos.SectionResponse(section.getId().toString(), section.getSectionKey(), section.getTitle(), section.getPurpose(),
                section.getContent(), section.getSourceType(), section.getEvidenceCoverage().doubleValue(),
                strings(section.getRetrievalQueries()), strings(section.getRequiredKnowledgeTypes()),
                section.getSectionStatus(), section.isLocked(), section.getCitations().stream().map(citation ->
                new SolutionDtos.EvidenceResponse(citation.getEvidenceId(), citation.getRagflowChunkId(), citation.getDatasetId(),
                        citation.getDocumentId(), citation.getDocumentName(), citation.getContentSnapshot(),
                        citation.getSimilarityScore() == null ? null : citation.getSimilarityScore().doubleValue(), citation.getPageNumber(),
                        citation.getAssetId() == null ? null : citation.getAssetId().toString(), citation.getSlideNumber())).toList(),
                section.getConfirmationReason());
    }

    private SolutionSectionEntity requireSection(UUID id) {
        return sectionRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "方案章节不存在"));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("无法序列化生成参数", exception); }
    }

    private List<String> strings(String value) {
        try { return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
        catch (Exception exception) { return List.of(); }
    }
}
