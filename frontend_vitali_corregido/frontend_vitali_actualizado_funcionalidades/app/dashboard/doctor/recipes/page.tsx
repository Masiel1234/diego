"use client";
import Link from "next/link";
export default function DoctorRecipesPage(){return <main className="mx-auto max-w-3xl space-y-4 p-6"><Link href="/dashboard/doctor" className="text-sm text-teal-700">← Volver al panel</Link><h1 className="text-2xl font-bold">Revisión de recetas</h1><p className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">El backend adjunto todavía no expone un endpoint para que el médico consulte las recetas asignadas y emita una revisión. No se muestra información simulada.</p></main>;}
