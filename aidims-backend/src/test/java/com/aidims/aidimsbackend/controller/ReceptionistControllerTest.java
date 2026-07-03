package com.aidims.aidimsbackend.controller;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.aidims.aidimsbackend.service.ReceptionistService;

@ExtendWith(MockitoExtension.class)
class ReceptionistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReceptionistService receptionistService;

    @InjectMocks
    private ReceptionistController receptionistController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(receptionistController).build();
    }

    @Test
    void getDoctorById_whenDoctorDoesNotExist_shouldReturnNotFound() throws Exception {
        Mockito.when(receptionistService.getDoctorById(999L))
                .thenThrow(new NoSuchElementException("No doctor found with ID 999"));
        mockMvc.perform(get("/api/receptionist/doctor/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
