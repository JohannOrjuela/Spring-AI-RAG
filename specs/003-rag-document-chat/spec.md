# Feature Specification: Chat con documentos usando RAG

**Feature Branch**: `[003-rag-document-chat]`

**Created**: 2026-10-28

**Status**: Implementación principal terminada; retos experimentales pendientes

**Input**: Construir sobre PruebaChat una experiencia local con Spring AI, Ollama y PGVector que permita cargar documentos, consultarlos mediante RAG y comparar las respuestas con un chat sin RAG.

## Clarifications

### Session 2026-10-28

- Q: ¿El modelo conversacional y el modelo de embeddings deben ser iguales? A: No. `gemma3:4b` genera respuestas y `nomic-embed-text` genera vectores de 768 dimensiones.
- Q: ¿La aplicación necesita visión? A: No para el alcance del taller. Gemma admite imágenes, pero los endpoints implementados aceptan documentos para extracción de texto y preguntas de texto.
- Q: ¿Cómo se compara RAG frente a la línea base? A: Mediante rutas y vistas separadas que reciben exactamente la misma pregunta.
- Q: ¿Qué formatos se aceptan? A: PDF, DOCX, PPTX, TXT, MD, HTML y HTM, hasta 20 MB y con texto extraíble.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Indexar un documento (Priority: P1)

Como estudiante, quiero cargar un documento desde el navegador para que su contenido quede disponible para búsquedas semánticas.

**Why this priority**: Sin ingesta no existe una fuente externa que el RAG pueda recuperar.

**Independent Test**: Cargar un archivo de texto con un dato único y verificar que el API responda `status=ok`, indique al menos un fragmento y que PGVector contenga la fila correspondiente.

**Acceptance Scenarios**:

1. **Given** un archivo válido con texto, **When** el usuario lo carga, **Then** el sistema extrae el texto, lo fragmenta, calcula embeddings y responde con archivo, fragmentos y tiempo.
2. **Given** un archivo vacío o con extensión no permitida, **When** se intenta cargar, **Then** el backend responde HTTP 400 y no indexa contenido.
3. **Given** un archivo válido mayor de 20 MB, **When** se intenta cargar, **Then** el backend rechaza la solicitud por exceder el límite.

---

### User Story 2 - Preguntar con RAG (Priority: P1)

Como usuario, quiero preguntar sobre los documentos cargados para recibir una respuesta fundamentada en los fragmentos más relevantes.

**Why this priority**: Es la funcionalidad principal evaluada por el taller.

**Independent Test**: Indexar un dato que no pertenezca al conocimiento común, preguntarlo en `/api/v1/chat/rag` y comprobar que la respuesta reproduce correctamente el dato.

**Acceptance Scenarios**:

1. **Given** fragmentos relevantes indexados, **When** llega una pregunta RAG, **Then** se recuperan hasta cuatro fragmentos con similitud mínima de 0.5 y Gemma responde usando el contexto.
2. **Given** una pregunta sin soporte en los documentos, **When** el contexto no contiene la respuesta, **Then** el asistente debe indicar que la información no se encuentra en el documento.
3. **Given** Ollama o PGVector no disponible, **When** se realiza la consulta, **Then** el frontend muestra un error y el backend registra la causa.

---

### User Story 3 - Comparar con chat sin RAG (Priority: P1)

Como estudiante, quiero realizar la misma pregunta sin recuperación para construir una línea base y medir el efecto del RAG.

**Why this priority**: La comparación sin RAG/con RAG es una evidencia obligatoria del taller.

**Independent Test**: Enviar una pregunta a `/api/v1/chat` y confirmar que el servicio responde sin ejecutar una búsqueda en PGVector.

**Acceptance Scenarios**:

1. **Given** el sistema disponible, **When** se consulta el chat general, **Then** Gemma responde sin recibir fragmentos del vector store.
2. **Given** una conversación en cada modo, **When** se cambia de vista, **Then** cada transcripción se conserva de manera independiente durante la sesión de la página.

---

### User Story 4 - Navegar entre tres vistas (Priority: P2)

Como usuario, quiero distinguir claramente la carga, el chat RAG y el chat general para evitar confundir los flujos del experimento.

**Independent Test**: Abrir el frontend y activar sucesivamente las tres opciones de navegación.

**Acceptance Scenarios**:

1. **Given** el frontend abierto, **When** se selecciona una opción, **Then** solo aparece la vista correspondiente.
2. **Given** el backend activo, **When** carga la página, **Then** el indicador lista los modelos disponibles en Ollama.
3. **Given** una pantalla estrecha, **When** se abre la interfaz, **Then** la navegación y los formularios permanecen utilizables.

---

### User Story 5 - Mostrar evidencia recuperada (Priority: P3)

Como usuario, quiero ver el archivo y un extracto de los fragmentos recuperados para verificar de dónde proviene la respuesta.

**Why this priority**: Aumenta la verificabilidad y corresponde al Reto 3.

**Independent Test**: Preguntar sobre un documento y comprobar que la respuesta del API incluya una lista no vacía de evidencias con el metadato `fuente`.

**Acceptance Scenarios**:

1. **Given** una respuesta sustentada, **When** el advisor recupera documentos, **Then** el API devuelve sus fuentes y extractos.
2. **Given** evidencia devuelta por el API, **When** el frontend renderiza la respuesta, **Then** muestra las fuentes sin interpretar el contenido como HTML.

**Implementation Status**: Pendiente.

## Edge Cases

- Documento escaneado sin texto seleccionable: Tika puede producir cero fragmentos y la carga debe fallar claramente.
- Documento cargado más de una vez: la versión actual duplica fragmentos; debe documentarse o incorporarse una estrategia de reemplazo.
- Cambio de modelo de embeddings: requiere recrear `vector_store` si cambia la dimensión.
- Pregunta demasiado general: puede recuperar fragmentos poco relacionados; el umbral debe filtrar ruido.
- Pregunta que requiere unir varias secciones: `topK` o el tamaño de fragmento pueden ser insuficientes.
- Primera inferencia en CPU: puede acercarse o superar el timeout del navegador.
- Nombre original nulo o extensión no admitida: la carga debe ser rechazada sin insertar datos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST ejecutar Ollama, PGVector, backend y frontend mediante Docker Compose.
- **FR-002**: El sistema MUST descargar o verificar `gemma3:4b` antes de iniciar el backend completo.
- **FR-003**: El sistema MUST usar un modelo dedicado de embeddings de 768 dimensiones.
- **FR-004**: El backend MUST aceptar documentos mediante `POST /api/v1/documents` con multipart key `file`.
- **FR-005**: El backend MUST aceptar PDF, DOCX, PPTX, TXT, MD, HTML y HTM de hasta 20 MB.
- **FR-006**: La ingesta MUST ejecutar Extract con Tika, Transform con `TokenTextSplitter` y Load con `VectorStore`.
- **FR-007**: Cada fragmento MUST guardar el nombre original bajo el metadato `fuente`.
- **FR-008**: El vector store MUST inicializar la extensión, tabla e índice PGVector automáticamente.
- **FR-009**: El chat RAG MUST recuperar `topK=4` con `similarityThreshold=0.5` como configuración base.
- **FR-010**: El sistema MUST exponer rutas diferentes para chat RAG y chat sin RAG.
- **FR-011**: El chat sin RAG MUST omitir el `QuestionAnswerAdvisor` y cualquier búsqueda vectorial.
- **FR-012**: El asistente RAG MUST responder en el idioma de la pregunta y evitar inventar cuando no exista soporte.
- **FR-013**: El frontend MUST ofrecer tres vistas identificables: carga, chat con RAG y chat sin RAG.
- **FR-014**: El frontend MUST mostrar estado de carga, fragmentos indexados, latencia y errores accionables.
- **FR-015**: El health endpoint MUST admitir solicitudes desde el frontend y listar los modelos de Ollama.
- **FR-016**: La documentación MUST incluir ejecución, API, comprobación SQL y limpieza del vector store.
- **FR-017**: El informe MUST comparar siete preguntas idénticas, incluyendo una fuera del documento.
- **FR-018**: Para el Reto 1, el advisor SHOULD usar una plantilla propia en español conservando sus marcadores obligatorios.
- **FR-019**: Para el Reto 2, el equipo MUST evaluar tres combinaciones de chunk, topK y umbral sobre las mismas preguntas.
- **FR-020**: Para el Reto 3, el API SHOULD exponer los documentos recuperados y el frontend SHOULD mostrar sus fuentes.

### Non-Functional Requirements

- **NFR-001**: La aplicación MUST ejecutarse completamente en local y no enviar documentos a servicios externos.
- **NFR-002**: El frontend MUST insertar respuestas mediante texto seguro y no mediante HTML generado por el modelo.
- **NFR-003**: Los contenedores MUST conservar modelos y datos en volúmenes persistentes.
- **NFR-004**: La interfaz MUST ser utilizable en escritorio y dispositivos móviles.
- **NFR-005**: La primera ejecución MAY tardar por las descargas; las siguientes MUST reutilizar volúmenes.

### Key Entities

- **UploadedDocument**: Archivo original, nombre, tipo, tamaño y momento de carga.
- **DocumentChunk**: Texto fragmentado con identificador y metadato `fuente`.
- **EmbeddingVector**: Representación numérica de 768 dimensiones asociada a un fragmento o pregunta.
- **ChatQuestion**: Pregunta, identificador de sesión y modo RAG/general.
- **ChatAnswer**: Texto, identificador de solicitud, latencia y estado.
- **RetrievedEvidence**: Documento recuperado, fuente, extracto y puntuación cuando esté disponible.
- **ExperimentRun**: Configuración de recuperación, cantidad de fragmentos, respuestas correctas y latencia promedio.

## Success Criteria *(mandatory)*

- **SC-001**: Un documento válido produce al menos un fragmento y queda consultable desde PGVector.
- **SC-002**: Una pregunta basada en un dato único del documento obtiene la respuesta correcta por la ruta RAG.
- **SC-003**: Las rutas RAG y general responden de forma independiente con un `status` explícito.
- **SC-004**: El proyecto compila y arranca con un único comando de Docker Compose.
- **SC-005**: Un usuario puede identificar y utilizar las tres vistas sin instrucciones externas.
- **SC-006**: El informe contiene siete comparaciones, tres experimentos y evidencia visual suficiente.
- **SC-007**: La pregunta fuera del documento es rechazada explícitamente por el modo RAG.

## Assumptions

- Docker Desktop dispone de memoria suficiente para Gemma 3 4B, Ollama, PostgreSQL y la JVM.
- El documento definitivo contiene texto seleccionable y no contiene información personal o confidencial.
- La latencia depende del hardware y la primera inferencia puede ser considerablemente más lenta.

## Done When

- Los pasos técnicos 2–8 están compilados y probados de extremo a extremo.
- El README contiene las evidencias del documento definitivo y no conserva campos `PENDIENTE`.
- Los tres retos se implementan o se documenta claramente cualquier limitación no resuelta.
- La tabla de evaluación y la tabla de experimentos están completas y sustentan las conclusiones.
