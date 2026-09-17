package com.documania.backend.common.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteReasonRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldAcceptValidReason() {
        var violations = validator.validate(new DeleteReasonRequest("Client demandé par courriel"));
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankReason() {
        var violations = validator.validate(new DeleteReasonRequest("   "));
        assertEquals(2, violations.size());
    }

    @Test
    void shouldRejectTooShortReason() {
        var violations = validator.validate(new DeleteReasonRequest("abc"));
        assertEquals(1, violations.size());
    }

    @Test
    void shouldRejectTooLongReason() {
        var violations = validator.validate(new DeleteReasonRequest("a".repeat(501)));
        assertEquals(1, violations.size());
    }
}
