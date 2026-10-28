package com.example.chat.rag;

import com.example.chat.dto.UploadResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "docx", "pptx", "txt", "md", "html", "htm");

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename();

        if (file.isEmpty() || !isAllowed(fileName)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(fileName, 0, 0, "error"));
        }

        long start = System.currentTimeMillis();
        int chunks = ingestionService.ingest(file.getResource(), fileName);
        long elapsed = System.currentTimeMillis() - start;
        return ResponseEntity.ok(new UploadResponse(fileName, chunks, elapsed, "ok"));
    }

    private boolean isAllowed(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
