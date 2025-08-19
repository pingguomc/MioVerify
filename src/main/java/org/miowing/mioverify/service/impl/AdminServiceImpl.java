package org.miowing.mioverify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.dao.AdminUserDao;
import org.miowing.mioverify.pojo.AdminUser;
import org.miowing.mioverify.pojo.Profile;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.service.AdminService;
import org.miowing.mioverify.service.ProfileService;
import org.miowing.mioverify.service.UserService;
import org.miowing.mioverify.util.PasswordUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AdminServiceImpl extends ServiceImpl<AdminUserDao, AdminUser> implements AdminService {
    
    @Autowired
    private PasswordUtil passwordUtil;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProfileService profileService;
    
    @Value("${mioverify.admin.default-username:admin}")
    private String defaultUsername;
    
    @Value("${mioverify.admin.default-password:admin123}")
    private String defaultPassword;
    
    @Override
    public AdminUser login(String username, String password) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUser::getUsername, username)
               .eq(AdminUser::getEnabled, true);

        AdminUser admin = getOne(wrapper);
        if (admin != null && passwordUtil.verifyPassword(password, admin.getPassword())) {
            // Update last login time
            admin.setLastLoginAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            updateById(admin);
            return admin;
        }
        return null;
    }
    
    @Override
    public void initializeDefaultAdmin() {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUser::getUsername, defaultUsername);

        if (getOne(wrapper) == null) {
            AdminUser defaultAdmin = new AdminUser()
                    .setId(Util.genUUID())
                    .setUsername(defaultUsername)
                    .setPassword(passwordUtil.encryptPassword(defaultPassword))
                    .setRole("ADMIN")
                    .setEnabled(true)
                    .setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            save(defaultAdmin);
            log.info("Default admin user created: {}", defaultUsername);
        }
    }
    
    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.count());
        stats.put("profileCount", profileService.count());
        stats.put("adminCount", count());
        stats.put("systemUptime", System.currentTimeMillis());
        return stats;
    }
    
    @Override
    public List<User> getUsers(int page, int size) {
        IPage<User> userPage = new Page<>(page, size);
        return userService.page(userPage).getRecords();
    }
    
    @Override
    public long getUserCount() {
        return userService.count();
    }
    
    @Override
    public List<Profile> getProfiles(int page, int size) {
        IPage<Profile> profilePage = new Page<>(page, size);
        return profileService.page(profilePage).getRecords();
    }
    
    @Override
    public long getProfileCount() {
        return profileService.count();
    }
    
    @Override
    public boolean deleteUser(String userId) {
        // Also delete associated profiles
        List<Profile> userProfiles = profileService.getByUserId(userId);
        for (Profile profile : userProfiles) {
            profileService.removeById(profile.getId());
        }
        return userService.removeById(userId);
    }
    
    @Override
    public boolean deleteProfile(String profileId) {
        return profileService.removeById(profileId);
    }
}
