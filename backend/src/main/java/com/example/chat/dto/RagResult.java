package com.example.chat.dto;

import java.util.List;

public record RagResult(String answer, List<SourceEvidence> sources) {}
