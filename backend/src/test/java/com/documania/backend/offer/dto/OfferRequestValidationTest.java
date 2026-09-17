package com.documania.backend.offer.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidCreateAndUpdateRequests() {
        assertTrue(validator.validate(validCreate()).isEmpty());
        assertTrue(validator.validate(new SaveOfferRequest(
            "Standard", null, BigDecimal.ZERO, 1, 1,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)
        )).isEmpty());
    }

    @Test
    void shouldRejectMissingRequiredValues() {
        assertFalse(validator.validate(new SaveOfferRequest(
            " ", null, null, 0, 0, null, null
        )).isEmpty());
    }

    @Test
    void shouldRejectNegativePriceAndNonPositiveQuantities() {
        assertFalse(validator.validate(new SaveOfferRequest(
            "Standard", null, new BigDecimal("-0.01"), 0, -1,
            LocalDate.now(), LocalDate.now()
        )).isEmpty());
    }

    @Test
    void shouldRejectPriceBeyondDatabasePrecision() {
        assertFalse(validator.validate(new SaveOfferRequest(
            "Standard", null, new BigDecimal("12345678901.999"), 1, 1,
            LocalDate.now(), LocalDate.now()
        )).isEmpty());
    }

    private SaveOfferRequest validCreate() {
        return new SaveOfferRequest(
            "Standard", null, new BigDecimal("99.90"), 12, 10,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
    }
}
