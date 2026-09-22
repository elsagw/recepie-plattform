package com.recipenetwork.backend.common;

import java.time.OffsetDateTime;

public record ErrorResponse(int status, String code, String message, String path, OffsetDateTime timestamp) {
}
