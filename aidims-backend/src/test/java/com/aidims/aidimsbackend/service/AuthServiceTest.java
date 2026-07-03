package com.aidims.aidimsbackend.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aidims.aidimsbackend.dto.LoginRequest;
import com.aidims.aidimsbackend.dto.LoginResponse;
import com.aidims.aidimsbackend.entity.Role;
import com.aidims.aidimsbackend.entity.User;
import com.aidims.aidimsbackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Unit Tests (Cross-role Login Bug Verification)")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Loi nghiep vu: Dang nhap sai vai tro yeu cau phai that bai (Dang nhap cheo vai tro)")
    void login_IncorrectRole_ShouldFail() {
        User mockUser = new User();
        mockUser.setUsername("doctor_user");
        mockUser.setPassword("doctorPass");
        mockUser.setActive(true);
        Role mockRole = new Role("doctor", "Doctor Role");
        mockUser.setRole(mockRole);

        when(userRepository.findByUsername("doctor_user")).thenReturn(Optional.of(mockUser));

        // Tài khoản có vai trò 'doctor', nhưng cố gắng đăng nhập với vai trò 'receptionist'.
        // AuthService phải từ chối đăng nhập chéo vai trò này.
        LoginRequest request = new LoginRequest();
        request.setUsername("doctor_user");
        request.setPassword("doctorPass");
        request.setRole("receptionist");

        LoginResponse response = authService.login(request);

        assertEquals("error", response.getStatus(), "Đăng nhập với tài khoản sai vai trò yêu cầu phải thất bại");
    }
}
