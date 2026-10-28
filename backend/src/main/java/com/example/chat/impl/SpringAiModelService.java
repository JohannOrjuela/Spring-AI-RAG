package com.example.chat.impl;

import com.example.chat.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Implementacion de ModelService sobre Spring AI.
 *
 * El starter spring-ai-starter-model-ollama autoconfigura el bean ChatClient.Builder
 * a partir de las propiedades spring.ai.ollama.*, por lo que aqui no hace falta
 * construir clientes HTTP, ni resolver endpoints, ni usar reflexion.
 */
@Service
public class SpringAiModelService implements ModelService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiModelService.class);

    private static final String GENERAL_SYSTEM_PROMPT = """
            Eres un asistente conciso y directo.
            Responde siempre en el mismo idioma en el que te escriba el usuario.
            Si no sabes algo, dilo en lugar de inventar.
            """;

    private static final String RAG_SYSTEM_PROMPT = """
            Eres un asistente que responde preguntas sobre los documentos cargados por el usuario.
            Responde siempre en el mismo idioma en el que te escriba el usuario.
            Usa solamente la informacion recuperada del documento como evidencia.
            Si la respuesta no esta en el contexto recuperado, indica claramente que no se encuentra en el documento.
            """;

    private final ChatClient generalChatClient;
    private final ChatClient ragChatClient;

    public SpringAiModelService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.generalChatClient = chatClientBuilder.clone()
                .defaultSystem(GENERAL_SYSTEM_PROMPT)
                .build();

        this.ragChatClient = chatClientBuilder.clone()
                .defaultSystem(RAG_SYSTEM_PROMPT)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(4)
                                .similarityThreshold(0.5)
                                .build())
                        .build())
                .build();
    }

    @Override
    public String generateResponse(String question, String sessionId) {
        log.debug("Enviando prompt al modelo. sessionId={} longitud={}", sessionId, question.length());

        return callModel(generalChatClient, question, sessionId, "sin-rag");
    }

    @Override
    public String generateRagResponse(String question, String sessionId) {
        return callModel(ragChatClient, question, sessionId, "rag");
    }

    private String callModel(ChatClient client, String question, String sessionId, String mode) {
        log.debug("Enviando prompt al modelo. mode={} sessionId={} longitud={}",
                mode, sessionId, question.length());

        String answer = client.prompt()
                .user(question)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            // El ChatController traduce una respuesta en blanco a HTTP 502 + status "error".
            log.warn("El modelo devolvio una respuesta vacia. mode={} sessionId={}", mode, sessionId);
            return "";
        }

        log.debug("Respuesta recibida. mode={} sessionId={} longitud={}", mode, sessionId, answer.length());
        return answer;
    }
}
