"use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {fetchApi} from "@/lib/api";
type Doctor={fullName:string;email:string;specialty:string;professionalRegistration:string;status:string;reviewedAt?:string|null};
export default function DoctorDashboard(){
 const [profile,setProfile]=useState<Doctor|null>(null);const [loading,setLoading]=useState(true);const [error,setError]=useState("");
 useEffect(()=>{fetchApi<Doctor>("/api/doctors/me").then(setProfile).catch(e=>setError(e.message)).finally(()=>setLoading(false));},[]);
 if(loading)return <main className="p-8">Cargando panel médico...</main>;
 return <main className="mx-auto max-w-5xl space-y-6 p-6"><header className="overflow-hidden rounded-3xl border bg-white shadow-sm"><div className="h-28 bg-gradient-to-r from-teal-700 via-teal-600 to-cyan-600"/><div className="p-6"><h1 className="text-2xl font-bold">Panel médico</h1>{profile&&<><p className="mt-2 text-slate-600">Bienvenido/a, {profile.fullName}</p><p className="text-sm text-slate-500">{profile.specialty} · Registro profesional: {profile.professionalRegistration}</p><p className="mt-3 text-sm">Estado de cuenta: <strong>{profile.status}</strong></p></>}</div></header>{error&&<p className="rounded-xl bg-red-50 p-3 text-red-700">{error}</p>}<section className="grid gap-4 sm:grid-cols-3"><Link href="/dashboard/doctor/appointments" className="rounded-2xl border bg-white p-5 hover:border-teal-300"><h2 className="font-bold">Solicitudes de citas</h2><p className="mt-1 text-sm text-slate-500">Revisa solicitudes de tus pacientes y confirma o rechaza.</p></Link><Link href="/dashboard/doctor/patients" className="rounded-2xl border bg-white p-5 hover:border-teal-300"><h2 className="font-bold">Mis pacientes</h2><p className="mt-1 text-sm text-slate-500">Ver pacientes asignados, tratamientos, citas y dudas individualmente.</p></Link><div className="rounded-2xl border bg-white p-5"><h2 className="font-bold">Tratamientos</h2><p className="mt-1 text-sm text-slate-500">Revisa los tratamientos desde cada ficha de paciente.</p></div></section></main>;
}
