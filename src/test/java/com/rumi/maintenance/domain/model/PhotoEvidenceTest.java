package com.rumi.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoEvidenceTest {

    @Test
    void acceptsAnHttpOrHttpsAddressAndTrimsIt() {
        assertThat(new PhotoEvidence("  https://storage.example.com/a.jpg ", null).url())
                .isEqualTo("https://storage.example.com/a.jpg");
        assertThat(new PhotoEvidence("HTTP://storage.example.com/a.jpg", null).url())
                .isEqualTo("HTTP://storage.example.com/a.jpg");
    }

    @Test
    void rejectsAMissingOrNonWebAddress() {
        assertThatThrownBy(() -> new PhotoEvidence(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL is required");
        assertThatThrownBy(() -> new PhotoEvidence("file:///C:/photos/crack.jpg", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL must be an http or https address");
        assertThatThrownBy(() -> new PhotoEvidence("not a url", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL must be an http or https address");
    }

    @Test
    void rejectsAnAddressLongerThanTheColumn() {
        String url = "https://storage.example.com/" + "a".repeat(480);

        assertThatThrownBy(() -> new PhotoEvidence(url, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL must have at most 500 characters");
    }
}
