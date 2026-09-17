package com.documania.backend.client.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateClientRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptRequiredFieldsAndMissingOptionalFields() {
        CreateClientRequest request = new CreateClientRequest(
            "contact@company.test", "Sara", "Amrani", "Company",
            null, null, null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectInvalidEmailAndBlankRequiredFields() {
        CreateClientRequest request = new CreateClientRequest(
            "invalid", " ", " ", " ", null, null, null
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
