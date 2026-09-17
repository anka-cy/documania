package com.documania.backend.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeStaffEnabledRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptTrueAndFalse() {
        assertTrue(validator.validate(new ChangeStaffEnabledRequest(true)).isEmpty());
        assertTrue(validator.validate(new ChangeStaffEnabledRequest(false)).isEmpty());
    }

    @Test
    void shouldRejectMissingEnabledValue() {
        assertEquals(
            1,
            validator.validate(new ChangeStaffEnabledRequest(null)).size()
        );
    }
}
