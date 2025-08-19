package org.miowing.mioverify.controller;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.pojo.AdminUser;
import org.miowing.mioverify.pojo.Profile;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin management controller
 */
@Controller
@RequestMapping("/admin")
@ConditionalOnProperty(name = "mioverify.admin.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    private static final String ADMIN_SESSION_KEY = "admin_user";
    
    /**
     * Admin login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
    
    /**
     * Admin login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, 
                                 @RequestParam String password, 
                                 HttpSession session) {
        AdminUser admin = adminService.login(username, password);
        if (admin != null) {
            session.setAttribute(ADMIN_SESSION_KEY, admin);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    /**
     * Admin logout
     */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(ADMIN_SESSION_KEY);
        return "redirect:/admin/login";
    }
    
    /**
     * Admin dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }
        
        Map<String, Object> stats = adminService.getSystemStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }
    
    /**
     * User management page
     */
    @GetMapping("/users")
    public String usersPage(Model model, HttpSession session,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int size) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }
        
        List<User> users = adminService.getUsers(page, size);
        long totalUsers = adminService.getUserCount();
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPages", (totalUsers + size - 1) / size);
        
        return "admin/users";
    }
    
    /**
     * Profile management page
     */
    @GetMapping("/profiles")
    public String profilesPage(Model model, HttpSession session,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }
        
        List<Profile> profiles = adminService.getProfiles(page, size);
        long totalProfiles = adminService.getProfileCount();
        
        model.addAttribute("profiles", profiles);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalProfiles", totalProfiles);
        model.addAttribute("totalPages", (totalProfiles + size - 1) / size);
        
        return "admin/profiles";
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/users/{userId}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable String userId, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        boolean deleted = adminService.deleteUser(userId);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    
    /**
     * Delete profile
     */
    @DeleteMapping("/profiles/{profileId}")
    @ResponseBody
    public ResponseEntity<?> deleteProfile(@PathVariable String profileId, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        boolean deleted = adminService.deleteProfile(profileId);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    
    /**
     * Get system statistics API
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return ResponseEntity.ok(adminService.getSystemStats());
    }
    
    /**
     * Check if admin is logged in
     */
    private boolean isAdminLoggedIn(HttpSession session) {
        return session.getAttribute(ADMIN_SESSION_KEY) != null;
    }
}
