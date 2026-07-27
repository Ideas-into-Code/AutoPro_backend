package com.autopro.backend.dto.upload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {

    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private long size;
    private String resourceType;
}
