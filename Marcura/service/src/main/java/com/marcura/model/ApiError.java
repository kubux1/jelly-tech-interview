package com.marcura.model;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiError(HttpStatus status, String message, Instant timestamp) {
}
