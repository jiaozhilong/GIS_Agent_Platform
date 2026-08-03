package com.jiaozhilong.gisagent.solution;

public final class SolutionEnums {
    private SolutionEnums() {}
    public enum TaskStatus { PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    public enum Stage { PLANNING, GENERATING, REVIEWING, COMPLETED }
    public enum GroundingPolicy { BALANCED, STRICT, CREATIVE }
    public enum SourceType { KNOWLEDGE_BASE, MODEL_GENERATED, HYBRID, PENDING_CONFIRMATION }
}
