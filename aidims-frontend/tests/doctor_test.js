Feature('doctor');

Before(({ I }) => {
  I.amOnPage('/login/doctor');
  I.fillField('Tên đăng nhập', '1');
  I.fillField('Mật khẩu', '1');
  I.click('Đăng nhập');
  I.waitForText('Chào mừng Bác sĩ', 10);
});

Scenario('Giao diện Dashboard Bác sĩ', ({ I }) => {
  I.see('Xem hồ sơ bệnh nhân');
  I.see('Xem và phân tích hình ảnh DICOM');
  I.see('Tạo báo cáo chẩn đoán');
  I.see('So sánh hình ảnh');
});

Scenario('Xem danh sách bệnh nhân và triệu chứng chi tiết', ({ I }) => {
  I.click('Xem hồ sơ bệnh nhân');
  I.seeInCurrentUrl('/doctor/patients');
  I.waitForElement('.patient-table', 10);
  I.click('.patient-row');
  I.waitForElement('.medical-record-modal', 10);
  I.click('Thông tin y tế');
  I.click('Xem triệu chứng');
  I.seeInCurrentUrl('/SymptomDisplay');
  I.see('Thông tin bệnh nhân');
});

Scenario('Tạo yêu cầu chụp chiếu X-quang/CT', ({ I }) => {
  I.click('Xem hồ sơ bệnh nhân');
  I.waitForElement('.patient-table', 10);
  I.click('.patient-row');
  I.waitForElement('.medical-record-modal', 10);
  I.click('Tạo yêu cầu chụp');
  I.seeInCurrentUrl('/CreateImagingRequest');
  I.selectOption('imagingType', 'x-ray');
  I.selectOption('bodyPart', 'Ngực');
  I.fillField('clinicalInfo', 'Ho dữ dội và đau ngực nhẹ');
  I.click('Gửi yêu cầu chụp');
});

Scenario('Tạo phiếu báo cáo chẩn đoán hình ảnh', ({ I }) => {
  I.click('Xem hồ sơ bệnh nhân');
  I.waitForElement('.patient-table', 10);
  I.click('.patient-row');
  I.waitForElement('.medical-record-modal', 10);
  I.click('Tạo báo cáo');
  I.seeInCurrentUrl('/MedicalReportForm');
  I.fillField('textarea[name=diagnosis]', 'Viêm phổi thùy phổi phải');
  I.fillField('textarea[name=findings]', 'Mờ hình kính ở thùy phải');
  I.fillField('textarea[name=recommendations]', 'Hẹn tái khám sau 2 tuần');
  I.click('.submit-button');
  I.waitForText('được lưu thành công', 10);
});

Scenario('Xem danh sách ảnh và chi tiết DICOM Viewer', ({ I }) => {
  I.click('Xem và phân tích hình ảnh DICOM');
  I.seeInCurrentUrl('/doctor/dicom-viewer');
  I.waitForElement('.dicom-table', 10);
  I.click('.dicom-row');
  I.waitForElement('.dicom-modal', 10);
  I.see('Chi tiết DICOM Viewer');
  I.click('Đóng');
});

Scenario('Tương tác trợ lý Chatbot AI', ({ I }) => {
  I.waitForElement('.chat-icon', 10);
  I.click('.chat-icon');
  I.waitForElement('.chat-window', 10);
  I.click('Test Gemini');
  I.waitForText('TEST GEMINI', 15);
  I.fillField('.input-textarea', 'Tư vấn đau ngực');
  I.click('.send-button');
  I.waitForElement('.message-bubble.bot', 15);
});
