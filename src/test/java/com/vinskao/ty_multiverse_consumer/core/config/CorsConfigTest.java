package com.vinskao.ty_multiverse_consumer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

/** 2026-10-31: the site moved to https://tymultiverse.cc; the old host only redirects pages now. */
class CorsConfigTest {

    private static CorsConfiguration cors() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "frontendUrl", "http://localhost:4321");
        return config.corsConfiguration();
    }

    @Test
    void allowsTheSiteAndTheConfiguredFrontend() {
        assertEquals("https://tymultiverse.cc", cors().checkOrigin("https://tymultiverse.cc"));
        assertEquals("http://localhost:4321", cors().checkOrigin("http://localhost:4321"));
    }

    @Test
    void rejectsTheRetiredHostAndUnrelatedSites() {
        assertNull(cors().checkOrigin("https://peoplesystem.tatdvsonorth.com"));
        assertNull(cors().checkOrigin("https://evil.example"));
    }
}
