# PruebaChat RAG

Chat local construido con Spring Boot 4.1, Spring AI 2.0, Ollama y PGVector. La interfaz permite indexar documentos y comparar la misma pregunta con recuperación RAG o sin ella.

## Servicios y modelos

- Chat: `gemma3:4b`.
- Embeddings: `nomic-embed-text` (768 dimensiones).
- Base vectorial: PostgreSQL 17 con PGVector.
- Frontend: HTML, CSS y JavaScript servido en el puerto 4200.

El modelo de chat y el modelo de embeddings no tienen que ser el mismo. Gemma genera respuestas; `nomic-embed-text` convierte preguntas y fragmentos en vectores comparables.

## Ejecución

Requiere Docker Desktop. Desde la raíz del proyecto:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

La primera ejecución descarga `gemma3:4b` y `nomic-embed-text`, por lo que puede tardar varios minutos. Después abre:

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Ollama: http://localhost:11434

Para revisar el estado:

```bash
docker compose -f docker/docker-compose.yml ps
docker logs -f pruebachat_backend
docker exec -it pruebachat_ollama ollama list
```

## Uso

1. En **Subir documentos**, selecciona un PDF, DOCX, PPTX, TXT, MD o HTML con texto seleccionable.
2. Espera la confirmación con el número de fragmentos indexados.
3. En **Chat con RAG**, pregunta sobre el contenido del documento.
4. Repite la pregunta en **Chat sin RAG** para obtener la línea base y comparar.

Subir dos veces un documento agrega nuevamente sus fragmentos. Para limpiar la base antes de otro experimento:

```bash
docker exec -it pruebachat_pgvector psql -U rag -d ragdb -c "TRUNCATE vector_store;"
```

## API

| Método | Ruta | Función |
|---|---|---|
| `POST` | `/api/v1/documents` | Indexa un archivo enviado como `multipart/form-data`, clave `file` |
| `POST` | `/api/v1/chat/rag` | Responde recuperando hasta 4 fragmentos desde PGVector |
| `POST` | `/api/v1/chat` | Responde sin consultar documentos |
| `GET` | `/health/llm` | Comprueba Ollama y lista los modelos descargados |

Los dos endpoints de chat reciben:

```json
{
  "question": "¿Qué dice el documento?",
  "sessionId": "grupo-1"
}
```

## Comprobar la indexación

```bash
docker exec -it pruebachat_pgvector psql -U rag -d ragdb -c "SELECT metadata->>'fuente' AS fuente, count(*) FROM vector_store GROUP BY 1;"
```

La configuración principal está en `backend/src/main/resources/application.properties`; los servicios están definidos en `docker/docker-compose.yml`.

---

## Informe del taller

> Esta sección está preparada para completar el entregable. Reemplace todos los campos marcados como **PENDIENTE** después de escoger el documento definitivo y ejecutar las pruebas.

### Integrantes y documento seleccionado

- **Integrantes:** PENDIENTE.
- **Documento:** PENDIENTE.
- **Número de páginas:** PENDIENTE.
- **Fuente o enlace:** PENDIENTE.
- **Motivo de selección:** PENDIENTE. Explique por qué contiene información suficientemente específica como para comprobar que las respuestas provienen del RAG y no de la memoria general del modelo.
- **Validación ética:** PENDIENTE. Confirme que no contiene datos personales, información confidencial ni material cuyo uso esté restringido.

### Arquitectura explicada con nuestras palabras

Cuando el usuario carga un archivo, el frontend lo envía al endpoint `/api/v1/documents`. Apache Tika extrae el texto y `TokenTextSplitter` lo divide en fragmentos de aproximadamente 500 tokens. `nomic-embed-text` convierte cada fragmento en un vector de 768 dimensiones. PGVector almacena el texto, su vector y el nombre del archivo como metadato.

Cuando el usuario pregunta en **Chat con RAG**, el sistema convierte la pregunta en un vector con el mismo modelo de embeddings. PGVector busca los cuatro fragmentos más parecidos y descarta los que no superan el umbral de similitud de 0.5. `QuestionAnswerAdvisor` agrega los fragmentos recuperados al contexto y finalmente `gemma3:4b` redacta la respuesta. En **Chat sin RAG**, la pregunta se envía directamente a Gemma sin consultar documentos, lo que permite obtener la línea base para la comparación.

### Preguntas de evaluación

Use exactamente las mismas preguntas en **Chat sin RAG** y **Chat con RAG**. Las preguntas 1 a 6 deben poder responderse desde el documento. Si alguna no se ajusta al documento elegido, cambie únicamente el elemento entre corchetes por un concepto equivalente del documento.

| # | Pregunta | Respuesta esperada y página | Respuesta sin RAG | Respuesta con RAG | ¿Correcta? |
|---:|---|---|---|---|---|
| 1 | ¿Cuál es el propósito u objetivo principal del documento? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 2 | ¿Qué problema, necesidad o situación motivó la creación del documento? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 3 | ¿Qué personas, instituciones o actores tienen responsabilidades según el documento y cuáles son? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 4 | ¿Cuáles son las etapas, acciones o requisitos principales del proceso descrito? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 5 | ¿Qué fecha, cantidad, plazo o valor específico establece el documento para [el aspecto más importante]? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 6 | ¿Qué excepción, limitación, condición especial o caso no permitido menciona el documento? | PENDIENTE (pág. __) | PENDIENTE | PENDIENTE | Sí / Parcial / No |
| 7 | ¿Cuál es la población actual de Mongolia y cómo cambió durante el último año? | No está en el documento | PENDIENTE | PENDIENTE | El resultado correcto del RAG es reconocer que no está en el documento |

Para que la evaluación sea verificable, copie las respuestas completas, anote la página que sustenta cada respuesta esperada y no cambie la redacción de las preguntas entre las dos vistas.

### Evidencias que se deben agregar

- [ ] Captura de la vista **Subir documentos** mostrando el archivo y el número de fragmentos indexados.
- [ ] Captura de al menos una respuesta correcta en **Chat con RAG**.
- [ ] Captura de la pregunta 7 mostrando que el sistema reconoce que la respuesta no está en el documento.
- [ ] Tabla anterior completamente diligenciada.
- [ ] Número de fragmentos almacenados, comprobado con la consulta SQL de la sección anterior.

## Retos del taller

### Reto 1: plantilla RAG en español y rechazo de respuestas sin soporte

**Objetivo:** reemplazar la plantilla interna en inglés de `QuestionAnswerAdvisor` por una plantilla propia en español. No basta con el `system prompt`: la plantilla del advisor es la que inserta la pregunta y los fragmentos recuperados en cada consulta.

La plantilla debe conservar los marcadores requeridos por Spring AI, ordenar que se use únicamente el contexto y definir una frase explícita para cuando la respuesta no esté sustentada. La prueba principal es la pregunta 7.

**Estado:** pendiente. Actualmente existe un `system prompt` restrictivo, pero todavía no se configuró `promptTemplate(...)` en el advisor.

### Reto 2: experimento de recuperación

**Objetivo:** medir cómo cambian los resultados al modificar el tamaño de fragmento, `topK` y `similarityThreshold`. Cada configuración requiere limpiar `vector_store`, reiniciar o reconstruir el backend si cambió el código, volver a cargar el documento y repetir las mismas siete preguntas.

Tabla para registrar el experimento:

| Configuración | `chunkSize` | `topK` | `similarityThreshold` | Fragmentos indexados | Correctas de 7 | Tiempo promedio |
|---|---:|---:|---:|---:|---:|---:|
| A - Base | 500 | 4 | 0.50 | PENDIENTE | PENDIENTE | PENDIENTE |
| B - Fragmentos pequeños | 250 | 6 | 0.55 | PENDIENTE | PENDIENTE | PENDIENTE |
| C - Fragmentos grandes | 800 | 3 | 0.60 | PENDIENTE | PENDIENTE | PENDIENTE |

**Conclusión del experimento:** PENDIENTE. Compare precisión, pérdida de contexto, ruido recuperado, cantidad de fragmentos y latencia. No afirme que una configuración es mejor sin relacionarlo con los resultados observados.

### Reto 3: mostrar la evidencia

**Objetivo:** hacer visible de dónde salió la respuesta. El backend debe recuperar `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS`, extender la respuesta del API con las fuentes y, opcionalmente, con un extracto de cada fragmento. El frontend debe mostrar esa evidencia debajo de la respuesta.

**Estado:** pendiente. Los metadatos `fuente` ya se guardan durante la ingesta, pero el endpoint de chat todavía devuelve únicamente el texto, el tiempo y el estado.

## Trabajo pendiente para terminar el taller

- [x] Pasos 2–8: infraestructura, dependencias, configuración, ingesta, endpoint, RAG y frontend.
- [ ] Paso 1: escoger y documentar el archivo definitivo y sus páginas.
- [ ] Paso 9: ejecutar y completar las siete preguntas sin RAG y con RAG.
- [ ] Resolver formalmente el Reto 1 con `promptTemplate(...)`.
- [ ] Ejecutar las tres configuraciones del Reto 2 y escribir una conclusión sustentada.
- [ ] Implementar el Reto 3 y mostrar las fuentes en la interfaz.
- [ ] Tomar las capturas indicadas en la sección de evidencias.
- [ ] Escribir limitaciones observadas y una mejora propuesta.
- [ ] Revisar que no queden campos `PENDIENTE` antes de entregar.

### Limitaciones observadas y mejora propuesta

- **Limitación observada:** PENDIENTE. Ejemplos válidos: latencia alta en CPU, documentos escaneados sin OCR, duplicación al cargar dos veces el mismo archivo o recuperación insuficiente para preguntas que requieren varias secciones.
- **Mejora propuesta:** PENDIENTE. Debe responder directamente a la limitación seleccionada y explicar cómo se implementaría.
