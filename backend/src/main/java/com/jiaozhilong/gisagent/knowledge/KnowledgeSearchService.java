package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.project.ProjectDtos;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSearchService {
    private final KnowledgeRetrievalService retrieval;

    public KnowledgeSearchService(KnowledgeRetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    public ProjectDtos.RetrievalResult search(ProjectDtos.RetrievalRequest request) {
        return retrieval.search(request);
    }
}
