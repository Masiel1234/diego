package com.example.backend_vitali.medication;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.backend_vitali.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Registro básico de recetas y consultas del módulo incorporado.
 * Los IDs se validan contra la tabla users compartida por el backend principal.
 */
@RestController
@RequestMapping("/api")
public class MedicationWorkflowController {
    private final JdbcTemplate jdbc;

    public MedicationWorkflowController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record RecipeRequest(Long patientId, List<RecipeItem> items) {}
    public record RecipeItem(String medicine, String indicatedDose, Long presentationId,
                             Double unitsPerDay, String consumptionUnit,
                             String indication, Integer treatmentDays) {}
    public record QuestionRequest(Long patientId, Long recipeId, String message) {}
    public record AnswerRequest(Long doctorId, String answer) {}

    @PostMapping("/recetas")
    @Transactional
    public ResponseEntity<?> crearReceta(@RequestBody RecipeRequest request) {
        if (request == null || request.patientId() == null ||
                request.items() == null || request.items().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Indique el paciente y al menos un medicamento."));
        }
        if (!userExists(request.patientId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El usuario paciente no existe."));
        }
        if (!request.patientId().equals(CurrentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Solo puedes consultar sobre tu propio tratamiento."));
        }
        for (RecipeItem item : request.items()) {
            if (item == null || item.presentationId() == null ||
                    item.unitsPerDay() == null || item.unitsPerDay() <= 0 ||
                    item.treatmentDays() == null || item.treatmentDays() <= 0 ||
                    item.consumptionUnit() == null || item.consumptionUnit().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cada medicamento requiere presentación, unidades por día, unidad y duración válidas."));
            }
            if (jdbc.queryForObject("SELECT COUNT(*) FROM presentaciones_medicamento WHERE id = ?",
                    Integer.class, item.presentationId()) == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "La presentación indicada no existe."));
            }
        }
        jdbc.update("INSERT INTO receta(id_paciente, fecha_subida, estado_analisis) VALUES (?, ?, ?)",
                request.patientId(), LocalDateTime.now().toString(), "PENDIENTE_REVISION_MEDICA");
        Long recipeId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        for (RecipeItem item : request.items()) {
            String medicine = jdbc.queryForObject(
                    "SELECT medicamento FROM presentaciones_medicamento WHERE id = ?",
                    String.class, item.presentationId());
            jdbc.update("""
                INSERT INTO detalle_receta
                (id_receta, nombre_medicamento, dosis_indicada, presentacion_id,
                 unidades_por_dia, unidad_consumo, indicacion, dias_tratamiento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, recipeId, medicine, item.indicatedDose(), item.presentationId(),
                    item.unitsPerDay(), item.consumptionUnit(), item.indication(), item.treatmentDays());
        }
        return ResponseEntity.ok(Map.of("recetaId", recipeId,
                "estado", "PENDIENTE_REVISION_MEDICA",
                "mensaje", "Receta registrada. Requiere revisión de un profesional de salud."));
    }

    @GetMapping("/recetas/paciente/{patientId}")
    public List<Map<String, Object>> recetasPaciente(@PathVariable Long patientId) {
        return jdbc.queryForList("""
            SELECT r.id, r.id_paciente, r.fecha_subida, r.estado_analisis,
                   d.id AS detalle_id, d.nombre_medicamento, d.dosis_indicada,
                   d.presentacion_id, d.unidades_por_dia, d.unidad_consumo,
                   d.indicacion, d.dias_tratamiento
            FROM receta r LEFT JOIN detalle_receta d ON d.id_receta = r.id
            WHERE r.id_paciente = ?
            ORDER BY r.id DESC, d.id
            """, patientId);
    }

    @PostMapping("/consultas")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> crearConsulta(@RequestBody QuestionRequest request) {
        if (request == null || request.patientId() == null ||
                request.message() == null || request.message().isBlank() ||
                request.message().length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Indique paciente y una duda de máximo 1000 caracteres."));
        }
        if (!userExists(request.patientId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El usuario paciente no existe."));
        }
        if (request.recipeId() != null && jdbc.queryForObject(
                "SELECT COUNT(*) FROM receta WHERE id = ? AND id_paciente = ?",
                Integer.class, request.recipeId(), request.patientId()) == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "La receta no pertenece al paciente indicado."));
        }
        jdbc.update("""
            INSERT INTO consulta_medica
            (id_paciente, id_receta, mensaje_duda, estado, fecha_consulta)
            VALUES (?, ?, ?, 'PENDIENTE', ?)
            """, request.patientId(), request.recipeId(), request.message().trim(),
                LocalDateTime.now().toString());
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        return ResponseEntity.ok(Map.of("consultaId", id, "estado", "PENDIENTE"));
    }

    @GetMapping("/consultas/paciente/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','FAMILY')")
    public List<Map<String, Object>> consultasPaciente(@PathVariable Long patientId) {
        validateAccessToPatient(patientId);
        return jdbc.queryForList("""
            SELECT id, id_paciente, id_medico, id_receta, mensaje_duda,
                   respuesta_medico, estado, fecha_consulta
            FROM consulta_medica WHERE id_paciente = ? ORDER BY id DESC
            """, patientId);
    }

    @PostMapping("/consultas/{consultationId}/responder")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> responderConsulta(@PathVariable Long consultationId,
                                                @RequestBody AnswerRequest request) {
        if (request == null || request.doctorId() == null ||
                request.answer() == null || request.answer().isBlank() ||
                request.answer().length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Indique médico y respuesta de máximo 1000 caracteres."));
        }
        Integer doctorCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND role = 'DOCTOR' AND status = 'ACTIVE'",
                Integer.class, request.doctorId());
        if (doctorCount == null || doctorCount == 0 || !request.doctorId().equals(CurrentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Solo puedes responder como el médico autenticado."));
        }
        int updated = jdbc.update("""
            UPDATE consulta_medica SET id_medico = ?, respuesta_medico = ?,
                   estado = 'ATENDIDA'
            WHERE id = ? AND estado = 'PENDIENTE'
              AND id_paciente IN (SELECT user_id FROM patient_profiles WHERE assigned_doctor_id = ?)
            """, request.doctorId(), request.answer().trim(), consultationId, request.doctorId());
        if (updated == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("consultaId", consultationId, "estado", "ATENDIDA"));
    }


    private void validateAccessToPatient(Long patientId) {
        Long current = CurrentUser.getId();
        String role = jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, current);
        if ("PATIENT".equals(role) && !current.equals(patientId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No puedes consultar otro paciente.");
        }
        if ("DOCTOR".equals(role)) {
            Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM patient_profiles WHERE user_id = ? AND assigned_doctor_id = ?", Integer.class, patientId, current);
            if (c == null || c == 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "El paciente no está asignado a este médico.");
        }
        if ("FAMILY".equals(role)) {
            Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM family_links WHERE patient_user_id = ? AND family_user_id = ? AND active = 1", Integer.class, patientId, current);
            if (c == null || c == 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No existe un vínculo familiar activo.");
        }
    }

    private boolean userExists(Long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}
