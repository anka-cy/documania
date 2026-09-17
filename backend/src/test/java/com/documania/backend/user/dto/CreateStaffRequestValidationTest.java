package com.documania.backend.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateStaffRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRequest() {
        CreateStaffRequest request = new CreateStaffRequest(
            "staff@documania.test",
            "Sara",
            "Amrani"
        );

        Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankFields() {
        CreateStaffRequest request = new CreateStaffRequest(" ", " ", " ");

        Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);

        Set<String> invalidFields = new HashSet<>();
        for (ConstraintViolation<CreateStaffRequest> violation : violations) {
            invalidFields.add(violation.getPropertyPath().toString());
        }

        assertEquals(Set.of("email", "firstName", "lastName"), invalidFields);
    }

    @Test
    void shouldRejectInvalidEmail() {
        CreateStaffRequest request = new CreateStaffRequest(
            "not-an-email",
            "Sara",
            "Amrani"
        );

        Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
            violation.getPropertyPath().toString().equals("email")
        ));
    }

    @Test
    void shouldRejectNamesThatAreTooLong() {
        String tooLongName = "a".repeat(101);
        CreateStaffRequest request = new CreateStaffRequest(
            "staff@documania.test",
            tooLongName,
            tooLongName
        );

        Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size());
    }
}
