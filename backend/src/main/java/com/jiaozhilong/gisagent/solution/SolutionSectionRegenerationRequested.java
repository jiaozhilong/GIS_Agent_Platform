package com.jiaozhilong.gisagent.solution;

import java.util.UUID;

public record SolutionSectionRegenerationRequested(UUID sectionId, String additionalInstruction) {}
