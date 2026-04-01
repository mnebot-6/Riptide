package com.mnebot.riptide.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSecurityHeaders() {
    install(DefaultHeaders) {
        // Prevent MIME type sniffing
        header("X-Content-Type-Options", "nosniff")
        // Prevent clickjacking
        header("X-Frame-Options", "DENY")
        // XSS protection (legacy browsers)
        header("X-XSS-Protection", "1; mode=block")
        // HSTS — enforce HTTPS for 1 year (only effective over HTTPS)
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        // Referrer policy — don't leak URLs
        header("Referrer-Policy", "no-referrer")
        // Content Security Policy — API only, no HTML
        header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        // Permissions Policy — disable all browser features
        header("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
    }
}
