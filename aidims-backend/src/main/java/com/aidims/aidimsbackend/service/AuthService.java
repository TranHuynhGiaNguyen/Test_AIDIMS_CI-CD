package com.aidims.aidimsbackend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aidims.aidimsbackend.dto.LoginRequest;
import com.aidims.aidimsbackend.dto.LoginResponse;
import com.aidims.aidimsbackend.entity.User;
import com.aidims.aidimsbackend.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        try {
            if (request == null) {
                return LoginResponse.error("Yêu cầu đăng nhập không hợp lệ");
            }

            String requestedRole = request.getRole();
            if (requestedRole == null || requestedRole.trim().isEmpty()) {
                return LoginResponse.error("Vai trò đăng nhập không được để trống");
            }

            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            
            if (userOpt.isEmpty()) {
                return LoginResponse.error("Tài khoản không tồn tại");
            }
            
            User user = userOpt.get();
            
            // So sánh mật khẩu plain text
            if (!user.getPassword().equals(request.getPassword())) {
                return LoginResponse.error("Mật khẩu không chính xác");
            }
            
            if (!user.isActive()) {
                return LoginResponse.error("Tài khoản đã bị vô hiệu hóa");
            }

            String actualRole = user.getRole() != null ? user.getRole().getRoleName() : null;
            if (actualRole == null || !actualRole.equalsIgnoreCase(requestedRole.trim())) {
                return LoginResponse.error("Tài khoản không thuộc vai trò yêu cầu");
            }
            
            // Tạo response
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole().getRoleName());
            userData.put("fullName", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            
            return LoginResponse.success(userData);
            
        } catch (Exception e) {
            return LoginResponse.error("Lỗi hệ thống: " + e.getMessage());
        }
    }
}