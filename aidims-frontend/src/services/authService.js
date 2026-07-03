const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080/api";

export const authService = {
  async login(username, password, role) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password, role }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Đăng nhập thất bại");
      }

      const data = await response.json();
      
      if (data.success || data.status === "success") {
        data.success = true;
        localStorage.setItem("user", JSON.stringify(data.data));
      }
      
      return data;
    } catch (error) {
      console.error("Login error:", error);
      throw error;
    }
  },

  // Các phương thức cụ thể cho từng role (nếu cần)
  async doctorLogin(username, password) {
    return this.login(username, password, 'doctor');
  },

  async adminLogin(username, password) {
    return this.login(username, password, 'admin');
  },

  async receptionistLogin(username, password) {
    return this.login(username, password, 'receptionist');
  },

  async technicianLogin(username, password) {
    return this.login(username, password, 'technician');
  },

  logout() {
    localStorage.removeItem("user");
  },

  getCurrentUser() {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  },

  isAuthenticated() {
    return this.getCurrentUser() !== null;
  }
};