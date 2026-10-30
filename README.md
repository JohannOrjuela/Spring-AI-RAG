# PruebaChat — Taller de chat con documentos usando RAG

> **Entrega — Universidad El Bosque · Aplicaciones basadas en grandes modelos de lenguaje**  
> Documento de referencia: *Manual demostrativo de préstamo de equipos – Biblioteca Aurora* (6 páginas, ficticio).

## 1. Integrantes y documento seleccionado

**Integrantes:**

- Daniel Felipe Agudelo Pérez
- Daniel Villamil
- Johan Sebastián Orjuela Heredia

**Fuente:** documento original ficticio de demostración, incluido en `documentos/Manual_demostrativo_Biblioteca_Aurora.pdf`; no corresponde a una biblioteca real. Tiene 6 páginas con texto seleccionable. Se eligió por sus normas concretas y poco conocidas por un LLM (horarios, límites, renovaciones, fallas, excepciones y reservas); así se puede evaluar si la respuesta proviene del contexto recuperado.

## 2. Arquitectura y flujo

**Ingesta:** el formulario de `frontend/src/index.html` envía el archivo a `POST /api/v1/documents`. `TikaDocumentReader` extrae el contenido. `TokenTextSplitter` lo divide mediante `withChunkSize`, se registra `fuente` en los metadatos, `nomic-embed-text` produce embeddings de 768 dimensiones y `VectorStore` los guarda en PostgreSQL 17 con PGVector.

**Consulta con RAG:** `POST /api/v1/chat/rag` usa `QuestionAnswerAdvisor`, que busca fragmentos por similitud con `topK` y `similarityThreshold`. Se agregan al prompt español, y `gemma3:4b` genera la respuesta. La implementación obtiene `RETRIEVED_DOCUMENTS` para exponer fuente y extracto en la interfaz. **Sin RAG:** `POST /api/v1/chat` consulta al modelo sin recuperar el PDF.

**Diferencia con el modelo sugerido en la guía:** se usa `gemma3:4b`, no `llama3`. No se realizó una comparación de modelos.

## 3. Ejecución

Con Docker Desktop en ejecución, abrir PowerShell desde `Spring-AI-RAG-main`:

```powershell
docker compose -f docker/docker-compose.yml up -d --build
docker compose -f docker/docker-compose.yml ps
docker exec pruebachat_backend printenv RAG_CHUNK_SIZE RAG_TOP_K RAG_SIMILARITY_THRESHOLD
```

Abrir `http://localhost:4200`, subir una única vez el PDF de `documentos/`, y probar en «Chat con RAG» y «Chat sin RAG». API en puerto `8080`; Ollama en `11434`; PostgreSQL en `5432`.

**Configuración final seleccionada:** `RAG_CHUNK_SIZE=500`, `RAG_TOP_K=2`, `RAG_SIMILARITY_THRESHOLD=0.5` (valores confirmados en terminal). Para confirmar los fragmentos realmente guardados:

```powershell
docker exec pruebachat_pgvector psql -U rag -d ragdb -c "SELECT metadata->>'fuente' AS fuente, COUNT(*) AS fragmentos FROM vector_store GROUP BY 1;"
```

Al cambiar `chunkSize`, los fragmentos previos no se actualizan solos. Es necesario respaldar primero la base, eliminar solo los registros del PDF experimental, reiniciar el backend con los valores nuevos y reindexar una vez. No ejecutar `docker compose down -v` para estos experimentos.

## 4. Paso 9 — Siete preguntas: sin RAG frente a con RAG

Resultados de las capturas y mediciones de una ejecución por pregunta. La columna «Con RAG» corresponde a la configuración de referencia **500 / 2 / 0.5**.

| # | Pregunta | Respuesta esperada (página) | Sin RAG (observado) | Con RAG (observado) | ¿Correcta con RAG? |
|---|---|---|---|---|---|
| 1 | ¿Cuál es el horario de entrega y devolución de equipos de la Biblioteca Aurora? | L–V, 08:30–17:00; sin entregas/devoluciones fines de semana (p. 1). | Afirmó incorrectamente que no se ofrecen entregas/devoluciones. | Correcta: horario y fines de semana. | Sí |
| 2 | ¿Cuál es el número máximo de equipos que puede tener prestados un usuario al mismo tiempo? | Máximo 2 equipos por usuario (p. 3). | Inventó máximo de 20 equipos. | Correcta: máximo 2. | Sí |
| 3 | ¿Cuántas veces se puede renovar un préstamo ordinario y por cuántos días adicionales? | Una vez; 2 días calendario extra, sin reserva posterior y antes de vencimiento (p. 3). | Inventó 3 renovaciones de 7 días. | Correcta: una renovación por 2 días y condiciones. | Sí |
| 4 | ¿Dónde y en qué plazo debe reportarse una falla detectada en un equipo prestado? | En el punto de atención, máximo 24 horas desde detección (p. 4). | Inventó pasos adicionales, aunque mencionó 24 horas. | Correcta: lugar y plazo de 24 horas. | Sí |
| 5 | ¿Se puede retirar del edificio un equipo LAB-EXCLUSIVO si existe autorización de la coordinación académica? | No, ni siquiera con autorización académica (p. 5). | Sugirió que podría permitirse según autorización. | Incorrecta: dijo que no encontraba información. | No |
| 6 | ¿Cuánto tiempo permanece activa una reserva ordinaria y qué ocurre si no se reclama el equipo? | 12 horas hábiles desde aviso; luego se libera automáticamente (p. 5). | No indicó el plazo; respuesta genérica. | Correcta: 12 horas hábiles y liberación. | Sí |
| 7 | ¿Cómo se llama el director de la Biblioteca Aurora? | No figura en el documento. | Inventó el nombre «Elias Thorne». | Correcta: reconoció información ausente. | Sí |

**Resultado:** con RAG, 6/7 respuestas correctas; sin RAG, 0/7 totalmente correctas bajo el criterio de fidelidad al manual (una respuesta contenía un dato parcialmente acertado, acompañado por contenido no sustentado). La pregunta 7 permite comprobar abstención ante datos ausentes.

## 5. Reto 1 — Prompt español y abstención

`SpringAiModelService.java` configura una plantilla `PromptTemplate` con `{query}` y `{question_answer_context}` y la asigna a `QuestionAnswerAdvisor.promptTemplate(...)`. Exige usar solo el contexto, respetar restricciones y, cuando falte soporte, escribir: **«No encuentro esa información en el documento cargado.»** En la prueba sobre el director (pregunta 7), el sistema con RAG se abstuvo correctamente; sin RAG inventó un nombre. Se observaron también fallos y respuestas contradictorias, por lo que esta protección no es infalible.

## 6. Reto 2 — Tres configuraciones de recuperación

| Configuración | chunkSize | topK | Umbral | Fragmentos indexados | Correctas / 7 | Parciales | Promedio por pregunta (s) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Referencia | 500 | 2 | 0.5 | 3 | 6/7 | 0 | 72.305 |
| A (pequeños) | 300 | 2 | 0.5 | 4 | 5/7 | 1 | 26.933 |
| C (grandes) | 800 | 3 | 0.6 | 2 | 2/7 | 0 | 26.061 |

**Nota metodológica:** las 7 preguntas se repitieron una sola vez en cada configuración y se midió el tiempo total mostrado por la interfaz. La configuración A varió únicamente `chunkSize` respecto de la referencia; la C varió los tres parámetros a la vez. La mejora o deterioro de tiempos **no permite inferir causalidad** sobre un parámetro sin más ensayos controlados. Los recuentos (3, 4 y 2) se consultaron en PGVector; la atribución a cada tamaño de fragmento depende de la limpieza y reindexación realizadas.

| Pregunta | Referencia: tiempo / evaluación | A: tiempo / evaluación | C: tiempo / evaluación |
|---|---|---|---|
| Horario | 84.138 s / Sí | 37.211 s / Sí | 56.981 s / Sí |
| Límite de equipos | 61.597 s / Sí | 23.218 s / Sí | 12.011 s / No |
| Renovación | 89.927 s / Sí | 24.607 s / Sí | 9.385 s / No |
| Reporte de fallas | 62.772 s / Sí | 24.732 s / Sí | 9.172 s / No |
| LAB-EXCLUSIVO | 68.124 s / No | 24.676 s / No | 40.388 s / No |
| Reserva ordinaria | 70.513 s / Sí | 30.439 s / Parcial | 15.106 s / No |
| Director (fuera del documento) | 69.061 s / Sí | 23.647 s / Sí | 39.384 s / Sí |

**Hallazgos:** la referencia obtuvo 6/7, A obtuvo 5/7 y C obtuvo 2/7. En A, la respuesta de reservas dio la duración correcta y la liberación automática, pero agregó contradictoriamente que esa información no estaba disponible (evaluación «Parcial»). La consulta LAB-EXCLUSIVO falló en las tres configuraciones. En C varias preguntas devolvieron 0 fragmentos recuperados aun estando el documento indexado; un umbral de 0.6 es una hipótesis plausible de filtrado, no una causa demostrada. Los tiempos bajos con 0 fuentes no implican mejor calidad.

## 7. Reto 3 — Evidencias de fuente

El backend accede a `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS` y construye información de fuentes con metadato `fuente` y extracto. En el frontend se muestra «Fuentes recuperadas (N fragmentos)» cuando existen. Esto representa **procedencia**, no una validación automática de veracidad. Se comprobó el comportamiento con capturas de preguntas con 2 fuentes y de preguntas sin fuentes.

## 8. Capturas y reproducibilidad

El documento Word complementario `Informe_evidencias_RAG.docx` contiene capturas disponibles de las pruebas A y C. El README presenta las tablas completas. Las capturas no se incorporan aquí de forma binaria al repositorio salvo que se añadan a `evidencias/capturas/`. **Las mediciones y respuestas de las pruebas se observaron en el equipo del grupo; este ZIP no representa una nueva ejecución del entorno Docker por parte del redactor del informe.**

## 9. Limitaciones y mejoras

- El modelo local en CPU presentó tiempos considerables y variables; los promedios proceden de solo 7 ejecuciones por configuración.
- LAB-EXCLUSIVO no se respondió correctamente pese a que la regla figura en el manual. Es necesario inspeccionar el texto completo de los fragmentos recuperados antes de culpar solo al umbral.
- Un documento cargado por segunda vez se agrega de nuevo; falta una operación de reemplazo automático por nombre/ID de documento.
- La configuración C cambió tres variables a la vez y no permite aislar su efecto individual.
- **Mejora propuesta:** construir un pequeño conjunto de evaluación automatizado que registre ID de fragmentos, puntuación de similitud, cobertura de evidencia y exactitud de respuesta; repetir mediciones bajo condiciones estables y probar los parámetros uno por uno.

## 10. Referencias

- Universidad El Bosque. *Taller: Chat con documentos usando RAG (Spring AI + PGVector)*, pp. 10–12 (Paso 9, Retos 1–3, entregables).
- Spring AI Reference: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- pgvector: https://github.com/pgvector/pgvector

**Antes de entregar:** completar nombres del grupo; revisar la corrección del conteo final (3 fragmentos tras restauración, si se reindexó); comprobar que el archivo Word complementario y las capturas correspondan a las ejecuciones documentadas.
