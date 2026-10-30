
package com.example.chat.impl;

import com.example.chat.ModelService;
import com.example.chat.dto.RagResult;
import com.example.chat.dto.SourceEvidence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpringAiModelService implements ModelService {

    private static final Logger log =
            LoggerFactory.getLogger(SpringAiModelService.class);

    private static final String NO_INFORMATION =
            "No encuentro esa información en el documento cargado.";

    // Chat sin RAG: no utiliza documentos.
    private static final String GENERAL_SYSTEM_PROMPT = """
            Eres un asistente conciso y directo.
            Responde en el mismo idioma del usuario.
            Si desconoces una respuesta, dilo.
            No inventes información.
            """;

    // Chat con RAG: utiliza únicamente el contexto recuperado.
    private static final String RAG_SYSTEM_PROMPT = """
            Eres un asistente de consulta documental.
            Responde en español.

            Utiliza exclusivamente la información recuperada
            de los documentos cargados.

            Si existe evidencia suficiente, responde directamente.

            Si no existe evidencia suficiente, indica que
            la información no está en el documento.

            Nunca inventes información ni presentes
            conocimiento general como evidencia documental.
            """;

    // Reto 1: plantilla personalizada en español.
    private static final PromptTemplate RAG_TEMPLATE =
            new PromptTemplate("""
                    PREGUNTA:
                    {query}

                    CONTEXTO DEL DOCUMENTO:
                    ---------------------
                    {question_answer_context}
                    ---------------------

                    INSTRUCCIONES:
                    1. Lee el contexto recuperado.
                    2. Responde usando únicamente ese contexto.
                    3. Si encuentras la respuesta, proporciona
                       la información precisa y completa.
                    4. Conserva las cantidades, plazos,
                       condiciones y restricciones relevantes.
                    5. Si el contexto no contiene la respuesta,
                       escribe exactamente:
                       No encuentro esa información en el documento cargado.
                    6. No combines una respuesta válida con
                       la frase de información no encontrada.
                    7. No inventes datos, nombres ni referencias.
                    8. Responde de forma breve, clara y en español.
                    """);

    private final ChatClient generalChatClient;
    private final ChatClient ragChatClient;

    public SpringAiModelService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            @Value("${rag.top-k:1}") int topK,
            @Value("${rag.similarity-threshold:0.5}") double threshold) {

        // Cliente sin recuperación documental.
        this.generalChatClient = chatClientBuilder.clone()
                .defaultSystem(GENERAL_SYSTEM_PROMPT)
                .build();

        // Cliente RAG con recuperación configurable.
        this.ragChatClient = chatClientBuilder.clone()
                .defaultSystem(RAG_SYSTEM_PROMPT)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(topK)
                                                .similarityThreshold(threshold)
                                                .build())
                                .promptTemplate(RAG_TEMPLATE)
                                .build())
                .build();

        log.info(
                "RAG configurado: topK={}, similarityThreshold={}",
                topK, threshold);
    }

    // Chat sin RAG.
    @Override
    public String generateResponse(
            String question,
            String sessionId) {

        return callModel(
                generalChatClient,
                question,
                sessionId,
                "sin-rag");
    }

    // Chat con RAG y evidencia de las fuentes.
    @Override
    public RagResult generateRagResponse(
            String question,
            String sessionId) {

        log.debug(
                "Consulta RAG. sessionId={} longitud={}",
                sessionId,
                question.length());

        ChatClientResponse response = ragChatClient
                .prompt()
                .user(question)
                .call()
                .chatClientResponse();

        if (response == null) {
            return new RagResult(
                    NO_INFORMATION,
                    List.of());
        }

        // Reto 3: obtener documentos recuperados.
        Object retrieved = response.context().get(
                QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

        List<SourceEvidence> sources =
                retrieved instanceof List<?> list
                        ? list.stream()
                                .filter(Document.class::isInstance)
                                .map(Document.class::cast)
                                .map(doc -> new SourceEvidence(
                                        String.valueOf(
                                                doc.getMetadata()
                                                        .getOrDefault(
                                                                "fuente",
                                                                "Fuente no identificada")),
                                        extractText(doc)))
                                .toList()
                        : List.of();

        String answer = "";

        if (response.chatResponse() != null
                && response.chatResponse().getResult() != null
                && response.chatResponse()
                        .getResult()
                        .getOutput() != null) {

            answer = response.chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        }

        // Sin evidencia recuperada no se admite
        // una respuesta presentada como documental.
        if (sources.isEmpty()) {
            answer = NO_INFORMATION;
        }

        if (answer == null || answer.isBlank()) {
            answer = NO_INFORMATION;
        }

        log.info(
                "Consulta RAG finalizada. Fuentes recuperadas={}",
                sources.size());

        return new RagResult(answer, sources);
    }

    // Extracto para mostrar en el frontend.
    private String extractText(Document doc) {

        String text = doc.getText();

        if (text == null || text.isBlank()) {
            return "";
        }

        return text.substring(
                0,
                Math.min(280, text.length()));
    }

    // Ejecución del chat sin RAG.
    private String callModel(
            ChatClient client,
            String question,
            String sessionId,
            String mode) {

        log.debug(
                "Enviando pregunta. mode={} sessionId={} longitud={}",
                mode,
                sessionId,
                question.length());

        String answer = client.prompt()
                .user(question)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {

            log.warn(
                    "Respuesta vacía. mode={} sessionId={}",
                    mode,
                    sessionId);

            return "";
        }

        log.debug(
                "Respuesta recibida. mode={} longitud={}",
                mode,
                answer.length());

        return answer;
    }
}

