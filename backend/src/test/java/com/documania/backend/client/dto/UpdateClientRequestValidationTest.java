package com.documania.backend.client.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateClientRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptRequiredFieldsAndNullOptionalFields() {
        UpdateClientRequest request = new UpdateClientRequest(
            "Sara", "Amrani", "Company", null, null, null
        );
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankRequiredFields() {
        UpdateClientRequest request = new UpdateClientRequest(
            " ", " ", " ", null, null, null
        );
        assertFalse(validator.validate(request).isEmpty());
    }
}
