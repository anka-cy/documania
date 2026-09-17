package com.documania.backend.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStaffRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidNames() {
        UpdateStaffRequest request = new UpdateStaffRequest("Sara", "Amrani");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankNames() {
        UpdateStaffRequest request = new UpdateStaffRequest(" ", " ");

        Set<ConstraintViolation<UpdateStaffRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size());
    }

    @Test
    void shouldRejectNamesLongerThanDatabaseColumns() {
        String tooLongName = "a".repeat(101);
        UpdateStaffRequest request = new UpdateStaffRequest(tooLongName, tooLongName);

        assertEquals(2, validator.validate(request).size());
    }
}
