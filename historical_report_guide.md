# Hướng dẫn tạo báo cáo kiểm thử JSON cho các tuần cũ hơn

Tài liệu này hướng dẫn cách áp dụng tính năng **JsonTestReportListener** vào các phiên bản (commit) cũ hơn của dự án AIDIMS để truy xuất báo cáo kiểm thử dạng JSON cho các tuần trước đó.

Có hai cách để thực hiện:
*   **Cách 1 (Khuyên dùng)**: Sử dụng các dòng lệnh Git để làm việc trực tiếp trên thư mục dự án hiện tại.
*   **Cách 2 (Thủ công)**: Sao chép thủ công các file cấu hình và chạy trên một thư mục clone riêng biệt.

> [!IMPORTANT]
> **Yêu cầu về Dependency**:
> Để tránh lỗi biên dịch `package org.junit.platform.launcher does not exist`, file `pom.xml` của tuần cũ cần được khai báo thêm thư viện `junit-platform-launcher`. Bạn hãy thêm đoạn mã sau vào thẻ `<dependencies>` trong file `pom.xml` của tuần cũ nếu nó chưa có:
> ```xml
> <dependency>
>     <groupId>org.junit.platform</groupId>
>     <artifactId>junit-platform-launcher</artifactId>
>     <scope>test</scope>
> </dependency>
> ```

---

## Cách 1: Sử dụng Git (Nhanh và tối ưu nhất)

Cách này giúp bạn thực hiện mọi thao tác trên chính thư mục hiện tại mà không cần sao chép file thủ công.

Giả sử nhánh chính hiện tại của bạn (chứa tính năng JSON report) tên là **`main`**.

### Bước 1: Tìm commit đại diện cho tuần cũ
Liệt kê danh sách commit của dự án để tìm mã hash của commit cuối tuần cũ cần xuất báo cáo:
```powershell
git log --oneline
```
*(Xác định mã commit của tuần cũ, ví dụ: `abc1234`)*

### Bước 2: Tạo một nhánh tạm thời từ commit cũ đó
Tạo và chuyển sang một nhánh tạm tên là `temp-report-week-x` dựa trên commit cũ:
```powershell
git checkout -b temp-report-week-x abc1234
```
Lúc này, toàn bộ mã nguồn của bạn đã quay trở về trạng thái của tuần cũ đó.

### Bước 3: Lấy tính năng báo cáo JSON từ nhánh `main` sang nhánh tạm
Chạy hai lệnh sau để kéo trực tiếp các file cấu hình báo cáo JSON từ nhánh `main` sang nhánh tạm hiện tại:
```powershell
# Kéo file Java listener sang
git checkout main -- aidims-backend/src/test/java/com/aidims/aidimsbackend/config/JsonTestReportListener.java

# Kéo file cấu hình đăng ký SPI sang
git checkout main -- aidims-backend/src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener
```

### Bước 4: Chạy test để sinh báo cáo
Mở terminal tại thư mục `aidims-backend` và chạy lệnh:
```powershell
mvn clean test
```

### Bước 5: Lưu trữ báo cáo của tuần cũ
Copy file JSON sinh ra tại `target/test-report.json` ra ngoài thư mục lưu trữ của bạn và đổi tên phù hợp:
*   Ví dụ: Lưu thành `reports/test-report-week-2.json`.

### Bước 6: Quay trở lại nhánh cũ và dọn dẹp
```powershell
# Quay lại nhánh hiện tại
git checkout main

# Xóa nhánh tạm thời vừa tạo
git branch -D temp-report-week-x
```

---

## Cách 2: Làm thủ công (Không sử dụng lệnh Git nâng cao)

Sử dụng cách này nếu bạn muốn clone một bản sao dự án cũ ra thư mục riêng biệt để thao tác.

### Bước 1: Lưu trữ 2 file cấu hình hiện tại ra ngoài
Copy hai file sau từ dự án hiện tại của bạn và dán tạm thời ra Desktop hoặc thư mục lưu trữ tạm:
1.  **File 1 (Mã nguồn Listener)**:
    `aidims-backend/src/test/java/com/aidims/aidimsbackend/config/JsonTestReportListener.java`
2.  **File 2 (File đăng ký SPI)**:
    `aidims-backend/src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`

### Bước 2: Chuẩn bị thư mục phiên bản cũ
Tải hoặc clone phiên bản của tuần cũ về máy ở một thư mục khác (ví dụ: đặt tên thư mục dự án cũ là `AIDIMS-tuần-2`).

### Bước 3: Dán 2 file cấu hình vào đúng cấu trúc thư mục của dự án cũ
Mở thư mục dự án cũ (`AIDIMS-tuần-2/aidims-backend`) và dán 2 file đã lưu ở Bước 1 vào đúng các vị trí:

1.  **Dán File 1 (Listener)**:
    *   Đi vào: `src/test/java/com/aidims/aidimsbackend/`
    *   Nếu chưa có thư mục `config`, hãy chuột phải tạo thư mục tên là `config`.
    *   Dán file `JsonTestReportListener.java` vào trong thư mục `config`.
2.  **Dán File 2 (SPI)**:
    *   Đi vào: `src/test/`
    *   Nếu chưa có thư mục `resources`, hãy tạo thư mục `resources`.
    *   Đi tiếp vào `resources`, tạo cấu trúc thư mục con: `META-INF/services/`.
    *   Dán file `org.junit.platform.launcher.TestExecutionListener` vào trong thư mục `services` đó.

### Bước 4: Chạy test và thu thập kết quả
1.  Mở Command Prompt hoặc PowerShell tại thư mục `AIDIMS-tuần-2/aidims-backend`.
2.  Chạy lệnh:
    ```powershell
    mvn clean test
    ```
3.  Sau khi test chạy xong, copy file `target/test-report.json` ra ngoài và đổi tên phù hợp (ví dụ: `report-tuan-2.json`).
