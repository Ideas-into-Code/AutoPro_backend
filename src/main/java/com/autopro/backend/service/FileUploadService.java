package com.autopro.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.autopro.backend.dto.upload.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    @Value("${upload.max-file-size}")
    private long maxFileSize;

    @Value("${upload.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${upload.allowed-document-types}")
    private String allowedDocumentTypes;

    public UploadResponse uploadProfilePicture(MultipartFile file) throws IOException {
        validateFile(file, allowedImageTypes);
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder",          "autopro/profiles",
                "resource_type",   "image",
                "transformation",  new Transformation().width(400).height(400).crop("fill").gravity("face"),
                "allowed_formats", "jpg,jpeg,png,webp"
        ));
        return buildResponse(result);
    }

    public UploadResponse uploadDocument(MultipartFile file) throws IOException {
        validateFile(file, allowedDocumentTypes);
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder",        "autopro/documents",
                "resource_type", "raw",
                "allowed_formats", "pdf"
        ));
        return buildResponse(result);
    }

    public void deleteFile(String publicId, String resourceType) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
    }

    /** Génère une URL signée avec expiration (en secondes). */
    public String generateSignedUrl(String publicId, int expiresInSeconds) {
        long expiresAt = System.currentTimeMillis() / 1000 + expiresInSeconds;
        return cloudinary.url()
                .signed(true)
                .generate(publicId + "?_exp=" + expiresAt);
    }

    private void validateFile(MultipartFile file, String allowedTypesConfig) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "Fichier trop volumineux. Taille max : " + (maxFileSize / 1024 / 1024) + " MB");
        }
        List<String> allowed = Arrays.asList(allowedTypesConfig.split(","));
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType.trim())) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé : " + contentType + ". Autorisés : " + allowed);
        }
    }

    private UploadResponse buildResponse(Map<?, ?> result) {
        return UploadResponse.builder()
                .publicId((String) result.get("public_id"))
                .url((String) result.get("url"))
                .secureUrl((String) result.get("secure_url"))
                .format((String) result.get("format"))
                .size(((Number) result.get("bytes")).longValue())
                .resourceType((String) result.get("resource_type"))
                .build();
    }
}
