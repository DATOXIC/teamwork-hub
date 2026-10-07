package com.teamwork.filters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CsrfFilterTest {

    @Test
    void tokenMustMatchExactly() {
        assertTrue(CsrfFilter.tokensMatch("abc123", "abc123"));
        assertFalse(CsrfFilter.tokensMatch("abc123", "abc124"));
        assertFalse(CsrfFilter.tokensMatch("abc123", ""));
        assertFalse(CsrfFilter.tokensMatch("abc123", null));
        assertFalse(CsrfFilter.tokensMatch(null, "abc123"));
    }

    @Test
    void onlyStateChangingMethodsAreChecked() {
        assertFalse(CsrfFilter.isStateChanging("GET"));
        assertFalse(CsrfFilter.isStateChanging("head"));
        assertFalse(CsrfFilter.isStateChanging("OPTIONS"));
        assertTrue(CsrfFilter.isStateChanging("POST"));
        assertTrue(CsrfFilter.isStateChanging("PUT"));
        assertTrue(CsrfFilter.isStateChanging("DELETE"));
    }
}
