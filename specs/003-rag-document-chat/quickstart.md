# Quickstart: Chat con documentos usando RAG

## Prerequisites

- Docker Desktop en ejecución.
- Al menos 8 GB de RAM disponible; más memoria mejora la experiencia.
- Acceso a red durante la primera descarga de imágenes y modelos.

## Start

Desde la raíz:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Abrir `http://localhost:4200`.

## Validate Services

```bash
docker compose -f docker/docker-compose.yml ps
docker exec -it pruebachat_ollama ollama list
docker exec -it pruebachat_pgvector psql -U rag -d ragdb -c "select version();"
```

La lista de Ollama debe incluir `gemma3:4b` y `nomic-embed-text`.

## Run the Workshop Flow

1. Limpiar datos de experimentos anteriores si corresponde.
2. Abrir **Subir documentos** e indexar el archivo definitivo.
3. Registrar el número de fragmentos.
4. Ejecutar las siete preguntas en **Chat sin RAG**.
5. Repetirlas sin cambiar su redacción en **Chat con RAG**.
6. Completar la tabla y tomar capturas.

## Inspect and Reset PGVector

```bash
docker exec -it pruebachat_pgvector psql -U rag -d ragdb -c "SELECT metadata->>'fuente' AS fuente, count(*) FROM vector_store GROUP BY 1;"
docker exec -it pruebachat_pgvector psql -U rag -d ragdb -c "TRUNCATE vector_store;"
```

Ejecutar `TRUNCATE` antes de cada configuración del Reto 2.

## Stop

```bash
docker compose -f docker/docker-compose.yml down
```

No añadir `-v` si se desean conservar modelos y datos.
