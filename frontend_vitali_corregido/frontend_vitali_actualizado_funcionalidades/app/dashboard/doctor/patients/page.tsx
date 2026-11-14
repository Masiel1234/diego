 "use client";
import {useEffect,useState} from "react";
import Link from "next/link";
import {fetchApi} from "@/lib/api";
type Patient={patientId:number;fullName:string;email:string;avatarUrl:string|null;birthDate:string|null;phone:string|null;weight:number|null;height:number|null;conditions:string|null;medications:string|null};
export default function DoctorPatients(){
 const [patients,setPatients]=useState<Patient[]>([]); const [loading,setLoading]=useState(true); const [error,setError]=useState("");
 useEffect(()=>{fetchApi<Patient[]>("/api/doctors/patients").then(setPatients).catch(e=>setError(e.message)).finally(()=>setLoading(false));},[]);
 return <main className="mx-auto max-w-6xl space-y-6 p-4 sm:p-6">
  <Link href="/dashboard/doctor" className="text-sm font-medium text-teal-700 hover:underline">← Volver al panel</Link>
  <header className="rounded-3xl bg-white p-6 shadow-sm border border-slate-200"><h1 className="text-2xl font-bold text-slate-900">Mis pacientes</h1><p className="mt-1 text-sm text-slate-500">Consulta cada paciente por separado, con sus tratamientos, citas y dudas.</p></header>
  {error&&<div className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</div>}
  {loading?<div className="rounded-2xl bg-white p-6">Cargando pacientes...</div>:patients.length===0?<div className="rounded-2xl border border-dashed p-10 text-center text-slate-500">No tienes pacientes asignados.</div>:
  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">{patients.map(p=><Link key={p.patientId} href={`/dashboard/doctor/patients/${p.patientId}`} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm hover:border-teal-300 hover:shadow-md transition"><div className="flex items-center gap-3"><div className="flex h-12 w-12 items-center justify-center rounded-full bg-teal-50 font-bold text-teal-700">{p.fullName.split(/\s+/).slice(0,2).map(x=>x[0]).join("").toUpperCase()}</div><div><h2 className="font-bold text-slate-900">{p.fullName}</h2><p className="text-xs text-slate-500">{p.email}</p></div></div><div className="mt-4 space-y-1 text-sm text-slate-600"><p>Medicamentos: {p.medications||"Sin registrar"}</p><p>Condiciones: {p.conditions||"Sin registrar"}</p></div><p className="mt-4 text-sm font-semibold text-teal-700">Ver información completa →</p></Link>)}</div>}
 </main>
}