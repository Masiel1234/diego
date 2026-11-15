package com.example.backend_vitali.config;

import com.example.backend_vitali.user.*;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializa datos ficticios en la base de datos cuando está vacía.
 * Crea usuarios de prueba (especialmente el administrador) para desarrollo.
 */
@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeData() {
        // Solo inicializar si no hay usuarios en la base de datos
        if (userRepository.count() == 0) {
            createAdminUser();
            createSampleUsers();
        }
    }

    private void createAdminUser() {
        AppUser admin = new AppUser(
            "Administrador del Sistema",
            "admin@vitali.com",
            passwordEncoder.encode("admin123"),
            Role.ADMIN,
            AccountStatus.ACTIVE
        );

        userRepository.save(admin);
        System.out.println("✅ Usuario administrador creado:");
        System.out.println("   Email: admin@vitali.com");
        System.out.println("   Contraseña: admin123");
    }

    private void createSampleUsers() {
        // Doctor de prueba
        AppUser doctor = new AppUser(
            "Dr. Juan Pérez",
            "doctor@vitali.com",
            passwordEncoder.encode("doctor123"),
            Role.DOCTOR,
            AccountStatus.ACTIVE
        );
        userRepository.save(doctor);

        // Paciente de prueba
        AppUser patient = new AppUser(
            "María García",
            "paciente@vitali.com",
            passwordEncoder.encode("paciente123"),
            Role.PATIENT,
            AccountStatus.ACTIVE
        );
        userRepository.save(patient);

        // Familiar de prueba
        AppUser family = new AppUser(
            "Carlos García",
            "familiar@vitali.com",
            passwordEncoder.encode("familiar123"),
            Role.FAMILY,
            AccountStatus.ACTIVE
        );
        userRepository.save(family);

        System.out.println("✅ Usuarios de prueba creados:");
        System.out.println("   Doctor: doctor@vitali.com / doctor123");
        System.out.println("   Paciente: paciente@vitali.com / paciente123");
        System.out.println("   Familiar: familiar@vitali.com / familiar123");
    }
}
