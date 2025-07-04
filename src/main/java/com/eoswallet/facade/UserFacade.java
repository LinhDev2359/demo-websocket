package com.eoswallet.facade;

import com.eoswallet.dto.UserRegistrationRequest;
import com.eoswallet.dto.UserRegistrationResponse;
import com.eoswallet.dto.UserLoginRequest;
import com.eoswallet.dto.UserLoginResponse;
import com.eoswallet.dto.UserProfileResponse;
import com.eoswallet.dto.UserProfileUpdateRequest;
import com.wallet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserFacade {
    
    UserRegistrationResponse registerUser(UserRegistrationRequest request);
    
    UserLoginResponse authenticateUser(UserLoginRequest request);
    
    UserProfileResponse getUserProfile(Long userId);
    
    UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);
    
    void deactivateUser(Long userId);
    
    void reactivateUser(Long userId);
    
    Page<UserProfileResponse> getAllUsers(Pageable pageable);
    
    boolean validateUserCredentials(String email, String password);
    
    void initiatePasswordReset(String email);
    
    void resetPassword(String token, String newPassword);
    
    void refreshUserSession(Long userId);
    
    UserProfileResponse promoteToAdmin(Long userId);
    
    UserProfileResponse demoteFromAdmin(Long userId);
}