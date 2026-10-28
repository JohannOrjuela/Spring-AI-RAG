# API Contract: Chat con documentos usando RAG

Base URL local: `http://localhost:8080`

## POST /api/v1/documents

Content-Type: `multipart/form-data`

Campo obligatorio: `file`.

Respuesta exitosa, HTTP 200:

```json
{
  "fileName": "manual.pdf",
  "chunks": 37,
  "elapsedMs": 4804,
  "status": "ok"
}
```

Archivo vacío o formato no permitido, HTTP 400:

```json
{
  "fileName": "imagen.png",
  "chunks": 0,
  "elapsedMs": 0,
  "status": "error"
}
```

## POST /api/v1/chat

Chat general sin recuperación.

## POST /api/v1/chat/rag

Chat con búsqueda semántica y contexto recuperado.

Solicitud compartida:

```json
{
  "question": "¿Cuál es el plazo indicado?",
  "sessionId": "grupo-1"
}
```

Respuesta exitosa, HTTP 200:

```json
{
  "requestId": "d31a82c8-5bf9-49e2-a8cc-1dd8a06b447d",
  "answer": "El plazo indicado es de 30 días.",
  "elapsedMs": 12500,
  "status": "ok"
}
```

Respuesta sin texto del modelo, HTTP 502:

```json
{
  "requestId": "d31a82c8-5bf9-49e2-a8cc-1dd8a06b447d",
  "answer": "",
  "elapsedMs": 12500,
  "status": "error"
}
```

## GET /health/llm

Comprueba el endpoint `/api/tags` de Ollama y devuelve los modelos presentes.

Respuesta exitosa, HTTP 200:

```json
{
  "ok": true,
  "endpoint": "http://ollama:11434/api/tags",
  "result": {
    "models": []
  }
}
```

## Planned Response Extension for Reto 3

La ruta RAG conservará los campos actuales y añadirá:

```json
{
  "evidence": [
    {
      "source": "manual.pdf",
      "excerpt": "El plazo máximo será de 30 días..."
    }
  ]
}
```

El cambio debe ser aditivo para no romper el frontend ni los clientes existentes.
