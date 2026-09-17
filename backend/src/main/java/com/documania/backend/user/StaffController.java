package com.documania.backend.user;

import com.documania.backend.user.dto.CreateStaffRequest;
import com.documania.backend.user.dto.StaffResponse;
import com.documania.backend.user.dto.ChangeStaffEnabledRequest;
import com.documania.backend.common.dto.DeleteReasonRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/accounts")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        UserAccount staffAccount = staffService.createStaff(request);
        return StaffMapper.toResponse(staffAccount);
    }

    @GetMapping
    public List<StaffResponse> listStaff() {
        List<StaffResponse> responses = new ArrayList<>();

        for (UserAccount staffAccount : staffService.listStaff()) {
            responses.add(StaffMapper.toResponse(staffAccount));
        }

        return responses;
    }

    @PatchMapping("/{id}/enabled")
    public StaffResponse changeStaffEnabled(
        @PathVariable UUID id,
        @Valid @RequestBody ChangeStaffEnabledRequest request
    ) {
        return StaffMapper.toResponse(staffService.changeStaffEnabled(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStaff(
        @PathVariable UUID id,
        @Valid @RequestBody DeleteReasonRequest request
    ) {
        staffService.deleteStaff(id, request.reason());
    }
}
