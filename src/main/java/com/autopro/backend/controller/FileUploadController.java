package com.autopro.backend.controller;

import com.autopro.backend.dto.upload.UploadResponse;
import com.autopro.backend.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Upload de photos de profil et documents")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload d'une photo de profil (JPEG, PNG, WEBP — max 5 MB)")
    public ResponseEntity<UploadResponse> uploadProfilePicture(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileUploadService.uploadProfilePicture(file));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload d'une image générique (photo de panne — JPEG, PNG, WEBP, max 5 MB)")
    public ResponseEntity<UploadResponse> uploadImage(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileUploadService.uploadImage(file));
    }

    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload d'un document PDF (max 5 MB)")
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileUploadService.uploadDocument(file));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Supprimer un fichier (Admin uniquement)")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "image") String resourceType) throws IOException {
        fileUploadService.deleteFile(publicId, resourceType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/signed-url")
    @Operation(summary = "Générer une URL signée pour un fichier privé")
    public ResponseEntity<String> getSignedUrl(
            @RequestParam String publicId,
            @RequestParam(defaultValue = "3600") int expiresInSeconds) {
        return ResponseEntity.ok(fileUploadService.generateSignedUrl(publicId, expiresInSeconds));
    }
}
