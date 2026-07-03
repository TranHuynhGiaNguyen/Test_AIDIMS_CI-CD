package com.aidims.aidimsbackend.service;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aidims.aidimsbackend.entity.Assignment;
import com.aidims.aidimsbackend.entity.Doctor;
import com.aidims.aidimsbackend.entity.Patient;
import com.aidims.aidimsbackend.repository.AssignmentRepository;
import com.aidims.aidimsbackend.repository.DoctorRepository;
import com.aidims.aidimsbackend.repository.PatientRepository;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public Assignment assignDoctor(Long patientId, Long doctorId, String department) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();

        if (doctor.getDepartment() == null || !doctor.getDepartment().equalsIgnoreCase(department)) {
            throw new IllegalArgumentException("Chuyên khoa không khớp!");
        }

        Assignment assignment = new Assignment();
        assignment.setPatient(patient);
        assignment.setDoctor(doctor);
        assignment.setDepartment(department);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus("Đang chờ");

        return assignmentRepository.save(assignment);
    }

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }
}