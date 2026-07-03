package com.aidims.aidimsbackend.controller;

import com.aidims.aidimsbackend.entity.Patient;
import com.aidims.aidimsbackend.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientRepository patientRepository;

    private Patient createPatient(Long id, Integer oxygenSaturation) {
        Patient patient = new Patient();
        patient.setPatient_id(id);
        patient.setPatient_code("BN001");
        patient.setFull_name("Nguyen Van A");
        patient.setOxygen_saturation(oxygenSaturation);
        return patient;
    }

    @Test
    void getAllPatients_shouldReturnOk() throws Exception {

        Patient patient = createPatient(1L, 98);

        when(patientRepository.findAll())
                .thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patient_id").value(1))
                .andExpect(jsonPath("$[0].patient_code").value("BN001"))
                .andExpect(jsonPath("$[0].oxygen_saturation").value(98));
    }

    @Test
    void getAllPatients_shouldReturnEmptyList() throws Exception {

        when(patientRepository.findAll())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getPatientById_shouldReturnPatient() throws Exception {

        Patient patient = createPatient(1L, 98);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_id").value(1))
                .andExpect(jsonPath("$.oxygen_saturation").value(98));
    }

    @Test
    void getPatientById_whenNotFound_shouldReturnNull() throws Exception {

        when(patientRepository.findById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void getPatientById_whenOxygenGreaterThan100_shouldClampTo100()
            throws Exception {

        Patient patient = createPatient(1L, 120);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oxygen_saturation").value(100));
    }

    @Test
    void getPatientById_whenOxygenLessThan0_shouldClampTo0()
            throws Exception {

        Patient patient = createPatient(1L, -10);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oxygen_saturation").value(0));
    }

    @Test
    void getPatientById_whenOxygenEquals100_shouldRemain100()
            throws Exception {

        Patient patient = createPatient(1L, 100);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oxygen_saturation").value(100));
    }

    @Test
    void getPatientById_whenOxygenEquals0_shouldRemain0()
            throws Exception {

        Patient patient = createPatient(1L, 0);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oxygen_saturation").value(0));
    }

    @Test
    void getPatientById_whenOxygenIsNull_shouldReturnPatient()
            throws Exception {

        Patient patient = createPatient(1L, null);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_id").value(1));
    }
}