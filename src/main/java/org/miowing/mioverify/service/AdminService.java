package org.miowing.mioverify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.miowing.mioverify.pojo.AdminUser;
import org.miowing.mioverify.pojo.Profile;
import org.miowing.mioverify.pojo.User;

import java.util.List;
import java.util.Map;

public interface AdminService extends IService<AdminUser> {
    
    /**
     * Admin login
     * @param username username
     * @param password password
     * @return admin user if login successful, null otherwise
     */
    AdminUser login(String username, String password);
    
    /**
     * Initialize default admin user
     */
    void initializeDefaultAdmin();
    
    /**
     * Get system statistics
     * @return statistics map
     */
    Map<String, Object> getSystemStats();
    
    /**
     * Get all users with pagination
     * @param page page number
     * @param size page size
     * @return user list
     */
    List<User> getUsers(int page, int size);
    
    /**
     * Get user count
     * @return total user count
     */
    long getUserCount();
    
    /**
     * Get all profiles with pagination
     * @param page page number
     * @param size page size
     * @return profile list
     */
    List<Profile> getProfiles(int page, int size);
    
    /**
     * Get profile count
     * @return total profile count
     */
    long getProfileCount();
    
    /**
     * Delete user by ID
     * @param userId user ID
     * @return true if deleted successfully
     */
    boolean deleteUser(String userId);
    
    /**
     * Delete profile by ID
     * @param profileId profile ID
     * @return true if deleted successfully
     */
    boolean deleteProfile(String profileId);
}
