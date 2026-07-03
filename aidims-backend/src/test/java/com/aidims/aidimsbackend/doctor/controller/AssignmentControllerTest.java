package com.aidims.aidimsbackend.doctor.controller;

import com.aidims.aidimsbackend.entity.Assignment;
import com.aidims.aidimsbackend.entity.Doctor;
import com.aidims.aidimsbackend.entity.Patient;
import com.aidims.aidimsbackend.entity.Symptom;
import com.aidims.aidimsbackend.service.ReceptionistService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidims.aidimsbackend.controller.AssignmentController;
@WebMvcTest(AssignmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceptionistService receptionistService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrUpdatePatient_shouldReturnOk() throws Exception {

        Patient patient = new Patient();
        patient.setPatient_id(1L);
        patient.setFull_name("Nguyen Van A");

        when(receptionistService.createOrUpdatePatient(any(Patient.class)))
                .thenReturn(patient);

        mockMvc.perform(post("/api/receptionist/patient")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient)))
                .andExpect(status().isOk());
    }

    @Test
    void recordSymptom_shouldReturnOk() throws Exception {

        Symptom symptom = new Symptom();
        symptom.setId(1L);
        symptom.setDescription("Headache");

        when(receptionistService.recordSymptom(1L, "Headache"))
                .thenReturn(symptom);

        mockMvc.perform(post("/api/receptionist/symptom")
                .param("patientId", "1")
                .param("description", "Headache"))
                .andExpect(status().isOk());
    }

    @Test
    void assignDoctor_shouldReturnOk() throws Exception {

        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDepartment("Radiology");

        when(receptionistService.assignDoctor(
                anyLong(),
                anyLong(),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(assignment);

        mockMvc.perform(post("/api/receptionist/assign")
                .param("patientId", "1")
                .param("doctorId", "2")
                .param("department", "Radiology")
                .param("priority", "HIGH")
                .param("notes", "Urgent"))
                .andExpect(status().isOk());
    }

    @Test
    void assignDoctor_withInvalidData_shouldReturnBadRequest() throws Exception {

        when(receptionistService.assignDoctor(
                anyLong(),
                anyLong(),
                anyString(),
                any(),
                any()))
                .thenThrow(new IllegalArgumentException("Invalid assignment"));

        mockMvc.perform(post("/api/receptionist/assign")
                .param("patientId", "1")
                .param("doctorId", "2")
                .param("department", "Radiology"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAssignments_shouldReturnOk() throws Exception {

        Assignment assignment = new Assignment();
        assignment.setId(1L);

        when(receptionistService.getAllAssignments())
                .thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/receptionist/assignments"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPatients_shouldReturnOk() throws Exception {

        Patient patient = new Patient();
        patient.setPatient_id(1L);
        patient.setFull_name("Nguyen Van A");

        when(receptionistService.getAllPatients())
                .thenReturn(List.of(patient));

        mockMvc.perform(get("/api/receptionist/patients"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllDoctors_shouldReturnOk() throws Exception {

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Doctor Test");
        doctor.setDepartment("Radiology");

        when(receptionistService.getAllDoctors())
                .thenReturn(List.of(doctor));

        mockMvc.perform(get("/api/receptionist/doctors"))
                .andExpect(status().isOk());
    }

    @Test
    void getDoctorsByDepartment_shouldReturnOk() throws Exception {

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Doctor Test");
        doctor.setDepartment("Radiology");

        when(receptionistService.getDoctorsByDepartment("Radiology"))
                .thenReturn(List.of(doctor));

        mockMvc.perform(get("/api/receptionist/doctors-by-department")
                .param("department", "Radiology"))
                .andExpect(status().isOk());
    }
}