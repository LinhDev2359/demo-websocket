package com.wallet.repository;

import com.wallet.entity.User;
import com.wallet.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Repository Interface
 * Data Access Layer cho User entity
 * 
 * Chức năng:
 * 1. CRUD operations cho User
 * 2. Custom queries cho business requirements
 * 3. Pagination support cho millions users
 * 4. Performance-optimized queries với indexes
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Tìm user theo username hoặc email
     * Sử dụng cho authentication
     * 
     * @param username username
     * @param email email
     * @return Optional User
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * Tìm user theo userId (external identifier)
     * 
     * @param userId external user ID
     * @return Optional User
     */
    Optional<User> findByUserId(String userId);
    
    /**
     * Tìm user theo email
     * 
     * @param email email address
     * @return Optional User
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Tìm user theo username
     * 
     * @param username username
     * @return Optional User
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check xem username đã tồn tại chưa
     * 
     * @param username username
     * @return true nếu tồn tại
     */
    boolean existsByUsername(String username);
    
    /**
     * Check xem email đã tồn tại chưa
     * 
     * @param email email
     * @return true nếu tồn tại
     */
    boolean existsByEmail(String email);
    
    /**
     * Check xem username hoặc email đã tồn tại chưa
     * 
     * @param username username
     * @param email email
     * @return true nếu tồn tại
     */
    boolean existsByUsernameOrEmail(String username, String email);
    
    /**
     * Tìm users theo status với pagination
     * 
     * @param status user status
     * @param pageable pagination info
     * @return Page of Users
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    
    /**
     * Tìm users được tạo trong khoảng thời gian
     * 
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination info
     * @return Page of Users
     */
    Page<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Tìm users theo email verified status
     * 
     * @param emailVerified email verification status
     * @param pageable pagination info
     * @return Page of Users
     */
    Page<User> findByEmailVerified(Boolean emailVerified, Pageable pageable);
    
    /**
     * Custom query để count users theo status
     * 
     * @param status user status
     * @return count
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);
    
    /**
     * Custom query để update last login time
     * Sử dụng cho performance optimization
     * 
     * @param userId user ID
     * @param lastLoginAt last login time
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.userId = :userId")
    int updateLastLoginTime(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    /**
     * Find active users with recent login
     * 
     * @param sinceDate date threshold
     * @param pageable pagination info
     * @return Page of Users
     */
    @Query("SELECT u FROM User u WHERE u.status = com.wallet.entity.UserStatus.ACTIVE AND u.lastLoginAt >= :sinceDate")
    Page<User> findActiveUsersWithRecentLogin(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);
}