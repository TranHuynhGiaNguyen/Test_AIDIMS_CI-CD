Feature('Receptionist Operations - Tiếp nhận & Tạo hồ sơ bệnh nhân');

Scenario('Lễ tân tiếp nhận bệnh nhân mới', ({ I }) => {

    const patientCode = 'BN999';
    const patientName = 'Nguyễn Hoàng Đức';

    // ==========================
    // ĐĂNG NHẬP
    // ==========================
    I.amOnPage('/login/receptionist');

    I.fillField('Tên đăng nhập', '1');
    I.fillField('Mật khẩu', '1');

    I.click('Đăng nhập');

    I.waitForText('Bắt đầu ca làm việc', 10);
    I.click('Bắt đầu ca làm việc');

    // Chờ dashboard load
    I.wait(2);

    // Vào trực tiếp trang quản lý bệnh nhân
    I.amOnPage('/receptionist/patients');

    I.seeInCurrentUrl('/receptionist/patients');

    // ==========================
    // FORM BỆNH NHÂN
    // ==========================
    I.waitForText('Quản lý Hồ sơ Bệnh nhân', 20);
    I.waitForElement('input[name="full_name"]', 10);

    // ==========================
    // THÔNG TIN CƠ BẢN
    // ==========================
    I.fillField('input[name="patient_code"]', patientCode);

    I.fillField(
        'input[name="full_name"]',
        patientName
    );

    I.fillField(
        'input[name="date_of_birth"]',
        '20-10-2004'
    );

    I.selectOption(
        'select[name="gender"]',
        'Nam'
    );

    I.fillField(
        'input[name="phone"]',
        '0916282912'
    );

    I.fillField(
        'input[name="email"]',
        'duc.nguyen@gmail.com'
    );

    I.fillField(
        'input[name="address"]',
        'Điện Bàn - Đà Nẵng'
    );

    // ==========================
    // THÔNG TIN Y TẾ
    // ==========================
    I.fillField(
        'input[name="identity_number"]',
        '044098001234'
    );

    I.fillField(
        'input[name="insurance_number"]',
        'GD479123456789'
    );

    I.fillField(
        'input[name="emergency_contact_name"]',
        'Nguyễn Văn B'
    );

    I.fillField(
        'input[name="emergency_contact_phone"]',
        '0935080444'
    );

    I.selectOption(
        'select[name="blood_type"]',
        'O+'
    );

    I.fillField(
        'textarea[name="allergies"]',
        'Dị ứng phấn hoa nhẹ'
    );

    I.fillField(
        'textarea[name="medical_history"]',
        'Tiền sử đau dạ dày nhẹ'
    );

    // ==========================
    // SINH HIỆU
    // ==========================
    I.fillField('input[name="temperature"]', '36.5');
    I.fillField('input[name="heart_rate"]', '72');

    I.fillField(
        'input[name="blood_pressure_systolic"]',
        '120'
    );

    I.fillField(
        'input[name="blood_pressure_diastolic"]',
        '80'
    );

    I.fillField(
        'input[name="respiratory_rate"]',
        '16'
    );

    I.fillField(
        'input[name="oxygen_saturation"]',
        '98'
    );

    I.fillField(
        'input[name="weight"]',
        '65'
    );

    I.fillField(
        'input[name="height"]',
        '170'
    );

    // ==========================
    // LƯU HỒ SƠ
    // ==========================
    I.click('button[type="submit"]');

    // Nếu có alert thành công
    I.acceptPopup();

    I.wait(5);

    // ==========================
    // TÌM KIẾM THEO MÃ BN
    // ==========================
    I.fillField(
        'input[placeholder*="Tìm kiếm"]',
        patientCode
    );

    I.wait(3);

    I.see(patientCode);
    I.see(patientName);

});