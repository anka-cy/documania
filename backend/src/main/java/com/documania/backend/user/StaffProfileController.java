package com.documania.backend.user;

import com.documania.backend.user.dto.ChangeStaffPasswordRequest;
import com.documania.backend.user.dto.StaffResponse;
import com.documania.backend.user.dto.UpdateStaffRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    public StaffProfileController(StaffProfileService staffProfileService) {
        this.staffProfileService = staffProfileService;
    }

    @GetMapping("/profile")
    public StaffResponse getProfile(Authentication authentication) {
        return StaffMapper.toResponse(staffProfileService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public StaffResponse updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateStaffRequest request
    ) {
        return StaffMapper.toResponse(staffProfileService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
        Authentication authentication,
        @Valid @RequestBody ChangeStaffPasswordRequest request
    ) {
        staffProfileService.changePassword(
            authentication.getName(),
            request.currentPassword(),
            request.newPassword(),
            request.newPasswordConfirmation()
        );
    }
}