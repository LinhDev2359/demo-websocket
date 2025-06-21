package com.wallet.service;

import com.wallet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Service Interface
 * Business Logic Layer cho User operations
 * 
 * Chức năng:
 * 1. User registration và profile management
 * 2. User authentication support
 * 3. User status management
 * 4. User statistics và reporting
 */
public interface UserService {
    
    /**
     * Tạo user mới
     * 
     * @param username username
     * @param email email
     * @param password plain text password
     * @param firstName first name
     * @param lastName last name
     * @return created User
     * @throws RuntimeException nếu username/email đã tồn tại
     */
    User createUser(String username, String email, String password, String firstName, String lastName);
    
    /**
     * Get user theo ID
     * 
     * @param userId user ID
     * @return Optional User
     */
    Optional<User> getUserById(String userId);
    
    /**
     * Get user theo username hoặc email
     * Sử dụng cho authentication
     * 
     * @param usernameOrEmail username hoặc email
     * @return Optional User
     */
    Optional<User> getUserByUsernameOrEmail(String usernameOrEmail);
    
    /**
     * Update user profile
     * 
     * @param userId user ID
     * @param firstName first name
     * @param lastName last name
     * @return updated User
     */
    User updateUserProfile(String userId, String firstName, String lastName);
    
    /**
     * Change user password
     * 
     * @param userId user ID
     * @param currentPassword current password
     * @param newPassword new password
     * @return true nếu thành công
     * @throws RuntimeException nếu current password không đúng
     */
    boolean changePassword(String userId, String currentPassword, String newPassword);
    
    /**
     * Update user status
     * 
     * @param userId user ID
     * @param status new status
     * @return updated User
     */
    User updateUserStatus(String userId, User.UserStatus status);
    
    /**
     * Verify user email
     * 
     * @param userId user ID
     * @return updated User
     */
    User verifyUserEmail(String userId);
    
    /**
     * Update last login time
     * 
     * @param userId user ID
     * @param lastLoginAt last login time
     */
    void updateLastLoginTime(String userId, LocalDateTime lastLoginAt);
    
    /**
     * Check xem username có available không
     * 
     * @param username username
     * @return true nếu available
     */
    boolean isUsernameAvailable(String username);
    
    /**
     * Check xem email có available không
     * 
     * @param email email
     * @return true nếu available
     */
    boolean isEmailAvailable(String email);
    
    /**
     * Get users với pagination
     * 
     * @param pageable pagination info
     * @return Page of Users
     */
    Page<User> getUsers(Pageable pageable);
    
    /**
     * Get users theo status
     * 
     * @param status user status
     * @param pageable pagination info
     * @return Page of Users
     */
    Page<User> getUsersByStatus(User.UserStatus status, Pageable pageable);
    
    /**
     * Get active users với recent login
     * 
     * @param days số ngày gần đây
     * @param pageable pagination info
     * @return Page of active Users
     */
    Page<User> getActiveUsersWithRecentLogin(int days, Pageable pageable);
    
    /**
     * Get user statistics
     * 
     * @return user statistics
     */
    UserStatistics getUserStatistics();
    
    /**
     * Inner class cho user statistics
     */
    class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long inactiveUsers;
        private final long suspendedUsers;
        private final long verifiedUsers;
        
        public UserStatistics(long totalUsers, long activeUsers, long inactiveUsers, 
                            long suspendedUsers, long verifiedUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
            this.suspendedUsers = suspendedUsers;
            this.verifiedUsers = verifiedUsers;
        }
        
        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
        public long getSuspendedUsers() { return suspendedUsers; }
        public long getVerifiedUsers() { return verifiedUsers; }
    }
}