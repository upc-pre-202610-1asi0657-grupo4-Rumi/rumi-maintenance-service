package com.rumi.maintenance.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;

/**
 * Photo that documents a damage or a finding. The service stores the address of the photo, not the file:
 * the client uploads the image to the file storage and sends its URL.
 *
 * @param url     http or https address of the photo
 * @param takenAt when the photo was taken; null when the client does not know it
 */
public record PhotoEvidence(String url, Instant takenAt) {

    public static final int MAX_URL_LENGTH = 500;

    public PhotoEvidence {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Photo URL is required");
        }
        url = url.trim();
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Photo URL must have at most " + MAX_URL_LENGTH + " characters");
        }
        if (!isWebAddress(url)) {
            throw new IllegalArgumentException("Photo URL must be an http or https address");
        }
    }

    private static boolean isWebAddress(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return scheme != null
                    && ("http".equals(scheme.toLowerCase(Locale.ROOT)) || "https".equals(scheme.toLowerCase(Locale.ROOT)))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
