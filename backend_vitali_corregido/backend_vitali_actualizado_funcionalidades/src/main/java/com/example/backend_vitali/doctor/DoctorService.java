package com.example.backend_vitali.doctor;

import com.example.backend_vitali.doctor.DoctorDtos.DoctorProfileResponse;
import com.example.backend_vitali.security.CurrentUser;
import com.example.backend_vitali.user.AccountStatus;
import com.example.backend_vitali.user.AppUser;
import com.example.backend_vitali.user.Role;
import com.example.backend_vitali.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.backend_vitali.patient.PatientRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.backend_vitali.appointment.AppointmentRepository;
import com.example.backend_vitali.doctor.DoctorDtos.DoctorPatientSummary;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final JdbcTemplate jdbc;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            PatientRepository patientRepository,
            JdbcTemplate jdbc,
            AppointmentRepository appointmentRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.jdbc = jdbc;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorPatientSummary> getMyPatients() {
        Long doctorId = CurrentUser.getId();
        getActiveUser(doctorId, Role.DOCTOR);
        return patientRepository.findByAssignedDoctor_Id(doctorId).stream()
                .map(p -> new DoctorPatientSummary(
                        p.getUser().getId(), p.getUser().getFullName(), p.getUser().getEmail(),
                        p.getAvatarUrl(), p.getBirthDate(), p.getPhone(), p.getWeight(),
                        p.getHeight(), p.getConditions(), p.getMedications()))
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String,Object> getPatientDetails(Long patientId) {
        Long doctorId = CurrentUser.getId();
        getActiveUser(doctorId, Role.DOCTOR);
        var p = patientRepository.findByUserId(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado."));
        if (p.getAssignedDoctor() == null || !doctorId.equals(p.getAssignedDoctor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El paciente no está asignado a este médico.");
        }
        var appointments = appointmentRepository.findByPatient_User_IdOrderByCreatedAtDesc(patientId);
        var consultations = jdbc.queryForList("""
                SELECT id, id_paciente, id_medico, id_receta, mensaje_duda, respuesta_medico, estado, fecha_consulta
                FROM consulta_medica WHERE id_paciente = ? ORDER BY id DESC
                """, patientId);
        var recipes = jdbc.queryForList("""
                SELECT r.id, r.fecha_subida, r.estado_analisis, d.nombre_medicamento, d.dosis_indicada,
                       d.unidades_por_dia, d.unidad_consumo, d.indicacion, d.dias_tratamiento
                FROM receta r LEFT JOIN detalle_receta d ON d.id_receta = r.id
                WHERE r.id_paciente = ? ORDER BY r.id DESC, d.id
                """, patientId);
        java.util.Map<String,Object> result = new java.util.HashMap<>();
        result.put("patient", new DoctorPatientSummary(patientId, p.getUser().getFullName(), p.getUser().getEmail(),
                p.getAvatarUrl(), p.getBirthDate(), p.getPhone(), p.getWeight(), p.getHeight(),
                p.getConditions(), p.getMedications()));
        result.put("appointments", appointments.stream().map(a -> {
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("id", a.getId()); m.put("requestedDateTime", a.getRequestedDateTime());
            m.put("scheduledDateTime", a.getScheduledDateTime()); m.put("reason", a.getReason());
            m.put("doctorNotes", a.getDoctorNotes()); m.put("status", a.getStatus().name());
            return m;
        }).toList());
        result.put("consultations", consultations);
        result.put("treatments", recipes);
        return result;
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getMyProfile() {
        Long userId = CurrentUser.getId();

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la cuenta."
                ));

        if (user.getRole() != Role.DOCTOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta operación está disponible únicamente para médicos."
            );
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no está activa."
            );
        }

        DoctorProfile profile = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el perfil médico."
                ));

        return new DoctorProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getProfessionalRegistration(),
                profile.getSpecialty(), profile.getDocumentType(), profile.getDocumentNumber(),
                profile.getPhone(), profile.getBirthDate(), profile.getUniversity(),
                profile.getGraduationYear(), profile.getYearsExperience(),
                profile.getConsultationMode(), profile.getProfessionalBio(),
                user.getStatus(),
                profile.getReviewedAt()
        );
    }
    private AppUser getActiveUser(Long userId, Role requiredRole) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la cuenta."
                ));

        if (user.getRole() != requiredRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario no tiene el rol requerido."
            );
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no está activa."
            );
        }

        return user;
    }
}
