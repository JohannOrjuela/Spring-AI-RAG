# Tasks: Chat con documentos usando RAG

**Input**: `spec.md`, `plan.md`, `data-model.md`, `contracts/api.md`, `quickstart.md`

## Phase 1 - Infrastructure

- [x] T001 Añadir servicio PGVector 17, credenciales, volumen y healthcheck.
- [x] T002 Cambiar el modelo conversacional a `gemma3:4b`.
- [x] T003 Añadir pull independiente de `nomic-embed-text`.
- [x] T004 Conectar el backend con PGVector dentro de la red Docker.
- [x] T005 Añadir dependencias de PGVector, advisor y Tika al Gradle.

## Phase 2 - Configuration

- [x] T006 Configurar chat, ventana de contexto y límites de generación.
- [x] T007 Configurar embeddings de 768 dimensiones.
- [x] T008 Configurar esquema, distancia coseno e índice HNSW.
- [x] T009 Configurar carga multipart de hasta 20 MB.

## Phase 3 - Document Ingestion

- [x] T010 Crear `IngestionService` con Extract, Transform y Load.
- [x] T011 Guardar el metadato `fuente` en cada fragmento.
- [x] T012 Rechazar documentos que no produzcan texto.
- [x] T013 Crear `UploadResponse`.
- [x] T014 Crear `POST /api/v1/documents` con validación de formato.

## Phase 4 - Dual Chat

- [x] T015 Crear cliente general sin advisor.
- [x] T016 Crear cliente RAG con topK 4 y umbral 0.5.
- [x] T017 Exponer `POST /api/v1/chat/rag`.
- [x] T018 Conservar `POST /api/v1/chat` como línea base.
- [x] T019 Añadir prompt de sistema restrictivo al modo RAG.

## Phase 5 - Frontend

- [x] T020 Crear vista de carga de documentos.
- [x] T021 Crear vista Chat con RAG.
- [x] T022 Crear vista Chat sin RAG.
- [x] T023 Mantener transcript y sesión separados por modo.
- [x] T024 Mostrar latencias, estado, fragmentos y errores.
- [x] T025 Añadir healthcheck visible y corregir CORS.
- [x] T026 Verificar navegación y layout responsive.

## Phase 6 - Validation

- [x] T027 Validar sintaxis de Docker Compose.
- [x] T028 Compilar backend y frontend en Docker.
- [x] T029 Comprobar inicialización de `vector_store`.
- [x] T030 Ejecutar smoke test de carga con documento sintético.
- [x] T031 Verificar respuesta RAG basada en dato único.
- [x] T032 Verificar chat general por ruta independiente.
- [x] T033 Eliminar fragmentos y archivos temporales del smoke test.

## Phase 7 - Workshop Completion

- [ ] T034 Escoger y documentar el archivo definitivo.
- [ ] T035 Ejecutar las siete preguntas en ambos modos.
- [ ] T036 Añadir capturas y completar tabla de resultados.
- [ ] T037 Implementar `promptTemplate(...)` propio en español.
- [ ] T038 Ejecutar las tres configuraciones del experimento.
- [ ] T039 Calcular precisión sobre 7 y latencia promedio.
- [ ] T040 Exponer documentos recuperados en la respuesta del API.
- [ ] T041 Mostrar fuentes y extractos en el frontend.
- [ ] T042 Redactar conclusión, limitación y mejora propuesta.
- [ ] T043 Eliminar todos los campos `PENDIENTE` del README.

## Dependencies

- T001–T009 bloquean la ingesta y el RAG.
- T010–T014 bloquean T016, T017 y las pruebas con documentos.
- T015–T019 bloquean la comparación del Paso 9.
- T034 bloquea T035–T043.
- T040 bloquea T041.
