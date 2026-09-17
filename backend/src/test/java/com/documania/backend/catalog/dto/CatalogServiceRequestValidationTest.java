package com.documania.backend.catalog.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogServiceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptRequiredNameAndNullOptionalFields() {
        assertTrue(validator.validate(
            new SaveCatalogServiceRequest("Cloud Backup", null, null)
        ).isEmpty());
        assertTrue(validator.validate(
            new SaveCatalogServiceRequest("Cloud Backup", null, null)
        ).isEmpty());
    }

    @Test
    void shouldRejectBlankName() {
        assertFalse(validator.validate(
            new SaveCatalogServiceRequest(" ", null, null)
        ).isEmpty());
        assertFalse(validator.validate(
            new SaveCatalogServiceRequest(" ", null, null)
        ).isEmpty());
    }

    @Test
    void shouldRejectValuesLongerThanDatabaseColumns() {
        assertFalse(validator.validate(
            new SaveCatalogServiceRequest("a".repeat(101), null, "b".repeat(101))
        ).isEmpty());
        assertFalse(validator.validate(
            new SaveCatalogServiceRequest("Valid", null, "b".repeat(101))
        ).isEmpty());
    }
}
