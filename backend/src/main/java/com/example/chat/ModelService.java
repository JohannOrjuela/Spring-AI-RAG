package com.example.chat;

public interface ModelService {
    /**
     * Generate a response from the chosen model.
     * Implementations should handle communication with Spring AI / model client.
     */
    String generateResponse(String question, String sessionId);

    /**
     * Generate a response grounded in the documents stored in PGVector.
     */
    String generateRagResponse(String question, String sessionId);
}
