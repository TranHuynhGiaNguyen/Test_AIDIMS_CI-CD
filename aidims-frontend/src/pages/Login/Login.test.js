import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Login from './Login';
import { authService } from '../../services/authService';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../../services/authService', () => ({
  authService: {
    login: jest.fn(),
  },
}));

describe('Login Component - Unit Tests (Bug Verification)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('❌ Lỗi điều hướng: Đăng nhập thành công với vai trò Tiếp nhận phải được chuyển thẳng đến dashboard (/receptionist hoặc /IndexReceptionist)', async () => {
    // Giả lập backend trả về kết quả đăng nhập thành công cho nhân viên tiếp nhận
    authService.login.mockResolvedValue({
      success: true,
      data: {
        userId: 3,
        username: '2',
        role: 'receptionist',
        fullName: 'Phạm Thị Tiếp Tân'
      }
    });

    render(<Login />);

    // Điền thông tin đăng nhập
    fireEvent.change(screen.getByPlaceholderText('Nhập tên đăng nhập'), { target: { value: '2' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu'), { target: { value: '2' } });

    // Click nút Đăng nhập
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    // Kỳ vọng: Sau 1 giây chuyển hướng người dùng đến trang tiếp nhận dashboard
    // Tuy nhiên thực tế code Login.js đang chuyển hướng về trang nhập vai trò "/login/receptionist" 
    // làm người dùng bị lặp lại thao tác đăng nhập.
    // Assertion này sẽ thất bại (FAIL), chỉ ra bug điều hướng.
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/receptionist');
    }, { timeout: 1500 });
  });

  test('❌ Lỗi hiển thị thông báo: Sai mật khẩu phải hiển thị thông báo lỗi chi tiết từ máy chủ, không được hiển thị lỗi kết nối mạng giả', async () => {
    // Giả lập API ném ra lỗi do sai mật khẩu
    authService.login.mockRejectedValue(new Error('Mật khẩu không chính xác'));

    render(<Login />);

    // Điền thông tin đăng nhập
    fireEvent.change(screen.getByPlaceholderText('Nhập tên đăng nhập'), { target: { value: 'doctor1' } });
    fireEvent.change(screen.getByPlaceholderText('Nhập mật khẩu'), { target: { value: 'wrong_pass' } });

    // Click nút Đăng nhập
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    // Kỳ vọng: Hệ thống phải hiển thị "Mật khẩu không chính xác" từ server
    // Tuy nhiên thực tế catch block của Login.js lại ghi đè thành thông báo lỗi kết nối mạng.
    // Assertion này sẽ thất bại (FAIL) do thông báo hiển thị là "Lỗi kết nối đến server..."
    await waitFor(() => {
      expect(screen.getByText('Mật khẩu không chính xác')).toBeInTheDocument();
    });
  });
});
