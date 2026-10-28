package com.example.chat.dto;

/** Result of indexing an uploaded document in the vector store. */
public record UploadResponse(String fileName, int chunks, long elapsedMs, String status) {}
