Feature('login');

Scenario('Đăng nhập Bác sĩ thất bại với mật khẩu sai', ({ I }) => {
  I.amOnPage('/login/doctor');
  I.see('Đăng nhập Bác sĩ');
  I.fillField('Tên đăng nhập', 'invalid_doctor_id');
  I.fillField('Mật khẩu', 'wrong_password');
  I.click('Đăng nhập');
  I.waitForText('❌', 5);
});

