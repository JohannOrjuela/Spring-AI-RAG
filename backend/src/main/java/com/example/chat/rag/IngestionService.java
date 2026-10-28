package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extracts, splits and indexes documents in PGVector. */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .withMinChunkSizeChars(200)
            .build();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(Resource resource, String fileName) {
        List<Document> documents = new TikaDocumentReader(resource).read();
        List<Document> chunks = splitter.apply(documents);

        List<Document> enriched = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                    metadata.put("fuente", fileName);
                    return new Document(chunk.getText(), metadata);
                })
                .toList();

        if (enriched.isEmpty()) {
            throw new IllegalArgumentException("El documento no contiene texto que se pueda indexar");
        }

        vectorStore.add(enriched);
        log.info("Documento '{}' indexado: {} fragmentos", fileName, enriched.size());
        return enriched.size();
    }
}
