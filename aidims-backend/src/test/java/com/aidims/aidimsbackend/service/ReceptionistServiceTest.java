package com.aidims.aidimsbackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aidims.aidimsbackend.entity.Patient;
import com.aidims.aidimsbackend.repository.AssignmentRepository;
import com.aidims.aidimsbackend.repository.DoctorRepository;
import com.aidims.aidimsbackend.repository.PatientRepository;
import com.aidims.aidimsbackend.repository.SymptomRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceptionistService - createOrUpdatePatient Unit Tests")
class ReceptionistServiceTest {

    @Mock private PatientRepository patientRepo;
    @Mock private SymptomRepository symptomRepo;
    @Mock private AssignmentRepository assignmentRepo;
    @Mock private DoctorRepository doctorRepo;
    @InjectMocks private ReceptionistService receptionistService;

    private Patient validPatient;

    @BeforeEach
    void setUp() {
        validPatient = new Patient();
        validPatient.setPatient_id(1L);
        validPatient.setPatient_code("BN001");
        validPatient.setFull_name("Nguyen Van A");
        validPatient.setGender("Nam");
        validPatient.setPhone("0901234567");
        // date_of_birth được set trong từng test case
    }

    // =============================================================
    // NHOM 1 - LOP HOP LE (EP + BVA)
    // =============================================================

    @Nested
    @DisplayName("Nhom 1 - Lop hop le (EP + BVA)")
    class HopLeTests {

        @Test
        @DisplayName("TC01 - Hop le: Ngay sinh 26 nam truoc (nominal) — Stmt 1,3,4,5,7,10,11")
        void tc01_Valid_Normal() {
            // Dung LocalDate.now().minusYears(26) de tranh phu thuoc ngay chay
            // Neu hardcode "2000-01-15" se cho tuoi 26 vao ngay 2026-07-03
            // Nhung se cho tuoi 25 neu chay truoc 15/01 hang nam
            String dob = LocalDate.now().minusYears(26).toString();
            validPatient.setDate_of_birth(dob);

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            assertEquals("BN001", result.getPatient_code());
            assertEquals(26, result.getAge());
            verify(patientRepo, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC02 - BVA: Tuoi = 0 (sinh hom nay) — Stmt 1,3,4,5,7,10,11 | Tag B1")
        void tc02_Valid_Age_0() {
            String today = LocalDate.now().toString();
            validPatient.setDate_of_birth(today);

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            assertEquals(0, result.getAge());
            verify(patientRepo, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC03 - BVA: Tuoi = 1 (sinh 1 nam truoc) — Stmt 1,3,4,5,7,10,11 | Tag B2")
        void tc03_Valid_Age_1() {
            String oneYearAgo = LocalDate.now().minusYears(1).toString();
            validPatient.setDate_of_birth(oneYearAgo);

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            assertEquals(1, result.getAge());
            verify(patientRepo, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC04 - BVA: Tuoi = 100 (sinh 100 nam truoc) — Stmt 1,3,4,5,7,10,11 | Tag B5")
        void tc04_Valid_Age_100() {
            String hundredYearsAgo = LocalDate.now().minusYears(100).toString();
            validPatient.setDate_of_birth(hundredYearsAgo);

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            assertEquals(100, result.getAge());
            verify(patientRepo, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC05 - Hop le: date_of_birth = null, age khong bi thay doi — Stmt 1,3,11 | Tag V2")
        void tc05_Valid_NullDateOfBirth() {
            validPatient.setDate_of_birth(null);
            // Khong setAge() -> age giu nguyen gia tri mac dinh cua entity (null hoac 0)

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            // Stmt 7 (setAge) KHONG duoc goi -> age giu nguyen
            // Khong assertEquals(null, age) vi kieu Integer co the la 0 tuy entity
            // -> Chi verify patientRepo.save duoc goi
            verify(patientRepo, times(1)).save(any(Patient.class));
            // Xac nhan setAge KHONG duoc goi (age khong bi tinh)
            // Neu Patient.age la null theo mac dinh thi:
            assertNull(result.getAge(),
                "Age phai giu nguyen null khi khong co ngay sinh");
        }

        @Test
        @DisplayName("TC06 - Hop le: date_of_birth = rong, age khong bi thay doi — Stmt 1,3,11 | Tag V3")
        void tc06_Valid_EmptyDateOfBirth() {
            validPatient.setDate_of_birth("");

            when(patientRepo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

            Patient result = receptionistService.createOrUpdatePatient(validPatient);

            assertNotNull(result);
            verify(patientRepo, times(1)).save(any(Patient.class));
            assertNull(result.getAge(),
                "Age phai giu nguyen null khi date_of_birth rong");
        }
    }

    // =============================================================
    // NHOM 2 - LOP KHONG HOP LE (EP)
    // =============================================================

    @Nested
    @DisplayName("Nhom 2 - Lop khong hop le (EP)")
    class KhongHopLeTests {

        @Test
        @DisplayName("TC07 - Patient = null -> IllegalArgumentException — Stmt 1,2 | Tag X1")
        void tc07_Invalid_PatientNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> receptionistService.createOrUpdatePatient(null));

            assertEquals("Thông tin bệnh nhân không hợp lệ!", ex.getMessage());
            verify(patientRepo, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC08 - Ngay sinh + 1 ngay (tuong lai gan) -> Exception — Stmt 1,3,4,5,6 | Tag X2,B6")
        void tc08_Invalid_FutureDate_PlusOne() {
            String futureDate = LocalDate.now().plusDays(1).toString();
            validPatient.setDate_of_birth(futureDate);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> receptionistService.createOrUpdatePatient(validPatient));

            assertEquals("Ngày sinh không thể ở tương lai!", ex.getMessage());
            verify(patientRepo, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC09 - Dinh dang sai dd-MM-yyyy -> DateTimeParseException — Stmt 1,3,4 | Tag X3")
        void tc09_Invalid_InvalidFormat_ddMMyyyy() {
            validPatient.setDate_of_birth("32-15-2025");

            assertThrows(DateTimeParseException.class,
                () -> receptionistService.createOrUpdatePatient(validPatient));

            verify(patientRepo, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC10 - Dinh dang sai yyyy/MM/dd -> DateTimeParseException — Stmt 1,3,4 | Tag X4")
        void tc10_Invalid_InvalidFormat_slash() {
            validPatient.setDate_of_birth("2025/01/15");

            assertThrows(DateTimeParseException.class,
                () -> receptionistService.createOrUpdatePatient(validPatient));

            verify(patientRepo, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("TC11 - Ngay sinh xa tuong lai (+ 100 nam) -> Exception — Stmt 1,3,4,5,6 | Tag X2")
        void tc11_Invalid_FarFutureDate() {
            String farFuture = LocalDate.now().plusYears(100).toString();
            validPatient.setDate_of_birth(farFuture);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> receptionistService.createOrUpdatePatient(validPatient));

            assertEquals("Ngày sinh không thể ở tương lai!", ex.getMessage());
            verify(patientRepo, never()).save(any(Patient.class));
        }
    }
}