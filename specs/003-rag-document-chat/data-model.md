# Data Model: Chat con documentos usando RAG

## UploadedDocument

Representa el archivo recibido antes de ser dividido.

| Campo | Tipo | Regla |
|---|---|---|
| `fileName` | string | Nombre original no vacío |
| `contentType` | string | Informativo; la extensión también se valida |
| `size` | long | Mayor que 0 y máximo 20 MB |
| `resource` | Resource | Debe ser legible por Tika |

## DocumentChunk

Persistido por Spring AI en `vector_store`.

| Campo | Tipo | Regla |
|---|---|---|
| `id` | UUID/string | Generado por Spring AI |
| `content` | text | Fragmento no vacío |
| `metadata.fuente` | string | Nombre del archivo original |
| `embedding` | vector(768) | Generado con `nomic-embed-text` |

## ChatRequest

| Campo | Tipo | Regla |
|---|---|---|
| `sessionId` | string | Correlación no sensible; puede ser nulo |
| `question` | string | Obligatorio y no blanco |
| `mode` | enum conceptual | Determinado por la ruta: `rag` o `general` |

## ChatResponse

| Campo | Tipo | Regla |
|---|---|---|
| `requestId` | string | UUID por respuesta |
| `answer` | string | No vacío si `status=ok` |
| `elapsedMs` | long | Tiempo medido en backend |
| `status` | string | `ok` o `error` |

## UploadResponse

| Campo | Tipo | Regla |
|---|---|---|
| `fileName` | string | Archivo procesado |
| `chunks` | int | Mayor que 0 si `status=ok` |
| `elapsedMs` | long | Tiempo de ingesta |
| `status` | string | `ok` o `error` |

## RetrievedEvidence *(planned)*

| Campo | Tipo | Regla |
|---|---|---|
| `source` | string | Tomado de `metadata.fuente` |
| `excerpt` | string | Extracto limitado y seguro |
| `score` | decimal nullable | Similitud si la API la expone |

## ExperimentRun

| Campo | Tipo | Regla |
|---|---|---|
| `chunkSize` | int | Una de las configuraciones evaluadas |
| `topK` | int | Fragmentos máximos recuperados |
| `similarityThreshold` | decimal | Entre 0 y 1 |
| `indexedChunks` | int | Medido tras reindexar |
| `correctAnswers` | int | Entre 0 y 7 |
| `averageLatencyMs` | decimal | Promedio de las siete consultas |

## State Transitions

```text
Archivo seleccionado → cargando → extraído → fragmentado → indexado
Pregunta RAG → embedding → recuperación → prompt aumentado → respuesta
Pregunta general → prompt directo → respuesta
```
