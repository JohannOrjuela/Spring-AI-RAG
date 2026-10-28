# Implementation Plan: Chat con documentos usando RAG

**Feature Directory**: `specs/003-rag-document-chat`

**Spec**: `specs/003-rag-document-chat/spec.md`

**Created**: 2026-10-28

## Summary

Extender PruebaChat con un pipeline RAG local. La solución conserva una ruta de chat general como línea base, añade carga ETL hacia PGVector, conecta un segundo `ChatClient` con `QuestionAnswerAdvisor` y ofrece tres vistas en un frontend estático.

## Technical Context

- Java 21, Spring Boot 4.1, Spring AI 2.0 y Gradle.
- Ollama con `gemma3:4b` para generación y `nomic-embed-text` para embeddings.
- PostgreSQL 17 con extensión PGVector, distancia coseno e índice HNSW.
- HTML, CSS y JavaScript sin framework, servido con `http-server`.
- Docker Compose como mecanismo único de orquestación.
- Tamaño base de chunk: 500 tokens; recuperación base: topK 4 y umbral 0.5.

## Constitution Check

- **Reproducibilidad local**: todos los servicios se declaran en Docker Compose y el README contiene un único comando de arranque.
- **Interfaces claras**: las rutas de documentos, chat RAG, chat general y salud se documentan en `contracts/api.md`.
- **Privacidad**: documentos y modelos permanecen en la máquina local.
- **Verificabilidad**: se conserva una línea base sin RAG y se especifica evidencia recuperada como evolución.
- **Observabilidad**: logs de ingesta, latencia por respuesta, estado de Ollama y errores visibles.

## Project Structure

```text
backend/src/main/java/com/example/chat/
├── ChatController.java
├── ModelService.java
├── dto/
├── health/
├── impl/
└── rag/
frontend/src/index.html
docker/docker-compose.yml
specs/003-rag-document-chat/
```

## Phase 0: Research and Decisions

- Separar generación y embeddings por responsabilidad.
- Elegir PGVector para mantener persistencia y búsqueda vectorial dentro de PostgreSQL.
- Usar advisor para insertar recuperación sin duplicar el controlador del chat.
- Mantener dos `ChatClient`: general y RAG.
- Excluir visión del alcance inicial aunque Gemma la soporte.

## Phase 1: Infrastructure

- Añadir PGVector, volumen y healthcheck.
- Añadir pulls independientes para el modelo conversacional y el de embeddings.
- Configurar dependencias de arranque del backend.

## Phase 2: Backend RAG

- Configurar datasource, dimensiones, distancia, índice, contexto y multipart.
- Implementar ingesta ETL y metadato de fuente.
- Implementar endpoint de documentos.
- Configurar chat general y chat RAG.
- Exponer rutas independientes y healthcheck con CORS.

## Phase 3: Frontend

- Crear navegación entre carga, RAG y general.
- Mantener transcript separado por modo.
- Mostrar progreso, latencia, modelos y errores.
- Aplicar layout responsive y render seguro con `textContent`.

## Phase 4: Validation

- Validar Docker Compose.
- Compilar imágenes del backend y frontend.
- Iniciar Ollama y PGVector.
- Indexar un documento sintético con un dato único.
- Verificar fila en `vector_store` y respuesta RAG correcta.
- Verificar ruta general por separado.
- Limpiar los datos sintéticos después de la prueba.

## Phase 5: Workshop Challenges

- Implementar plantilla RAG propia en español.
- Ejecutar tres configuraciones controladas de recuperación.
- Extraer `RETRIEVED_DOCUMENTS`, extender el contrato y renderizar evidencia.
- Completar tabla, capturas, limitaciones y conclusión.

## Risks and Mitigations

- **Latencia en CPU**: timeout de 180 segundos y mensajes de progreso.
- **Duplicados**: documentar limpieza; futura sustitución por filtro de fuente.
- **PDF escaneado**: rechazar cero fragmentos y recomendar OCR u otro archivo.
- **Cambio de dimensión**: recrear tabla antes de cambiar modelo de embeddings.
- **CORS entre 4200 y 8080**: anotaciones explícitas en todos los controladores consumidos por el frontend.
