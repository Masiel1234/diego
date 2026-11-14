export type Role = 'ADMIN' | 'DOCTOR' | 'PATIENT' | 'FAMILY';

export interface UserResponse {
  id: number;
  email: string;
  role: Role;
  token?: string;
}

export interface LoginResponse {
  accessToken: string;
  user: UserResponse & { status: string; fullName: string };
}

export interface PatientProfile {
  id: number;
  medicalHistory?: string;
  emergencyContact?: string;
}

export interface DoctorProfile {
  id: number;
  specialty: string;
  licenseNumber: string;
  approved: boolean;
}