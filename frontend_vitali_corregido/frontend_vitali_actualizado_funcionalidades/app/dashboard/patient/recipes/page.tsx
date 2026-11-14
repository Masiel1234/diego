"use client";
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { fetchApi } from "@/lib/api";
import { ArrowLeft, Pill, Plus, RefreshCw, Search } from "lucide-react";

type CatalogMedicine = { medicamento: string; active_ingredient?: string; purpose?: string; dosage_form?: string; route?: string; structured_data_status?: string; };
type Presentation = { id:number; medicamento:string; forma:string; via:string; concentration_value:number|string; concentration_unit:string; denominator?:string; source_label?:string; source_url?:string; };
type RecipeRow = { id:number; id_paciente:number; fecha_subida:string; estado_analisis:string; detalle_id:number|null; nombre_medicamento:string|null; dosis_indicada:string|null; presentacion_id:number|null; unidades_por_dia:number|null; unidad_consumo:string|null; indicacion:string|null; dias_tratamiento:number|null; };
type Item = { medicine:string; indicatedDose:string; presentationId:number; unitsPerDay:string; consumptionUnit:string; indication:string; treatmentDays:string; };

const blank:Item={medicine:"",indicatedDose:"",presentationId:0,unitsPerDay:"1",consumptionUnit:"tableta",indication:"",treatmentDays:"7"};
const dateLabel=(v:string)=>v?new Date(v).toLocaleString("es-CO",{dateStyle:"medium",timeStyle:"short"}):"Fecha no disponible";

export default function PatientRecipesPage(){
 const [catalog,setCatalog]=useState<CatalogMedicine[]>([]);
 const [presentations,setPresentations]=useState<Presentation[]>([]);
 const [recipes,setRecipes]=useState<RecipeRow[]>([]);
 const [query,setQuery]=useState(""); const [selected,setSelected]=useState("");
 const [item,setItem]=useState<Item>(blank); const [items,setItems]=useState<Item[]>([]);
 const [loading,setLoading]=useState(true); const [saving,setSaving]=useState(false); const [error,setError]=useState(""); const [message,setMessage]=useState("");
 const userId=typeof window!=="undefined"?Number(localStorage.getItem("userId")):0;
 const load=useCallback(async()=>{
   setLoading(true);setError("");
   try{
     const [c,r]=await Promise.all([
       fetchApi<CatalogMedicine[]>("/api/medicamentos"),
       userId?fetchApi<RecipeRow[]>(`/api/recetas/paciente/${userId}`):Promise.resolve([])
     ]);
     setCatalog(Array.isArray(c)?c:[]);setRecipes(Array.isArray(r)?r:[]);
   }catch(e){setError(e instanceof Error?e.message:"No se pudieron cargar los medicamentos.");}
   finally{setLoading(false);}
 },[userId]);
 useEffect(()=>{void load();},[load]);
 useEffect(()=>{if(!selected){setPresentations([]);return;} fetchApi<Presentation[]>(`/api/medicamentos/presentaciones?medicamento=${encodeURIComponent(selected)}`).then(v=>setPresentations(Array.isArray(v)?v:[])).catch(e=>setError(e.message));},[selected]);
 const matches=useMemo(()=>catalog.filter(m=>`${m.medicamento} ${m.active_ingredient||""}`.toLowerCase().includes(query.toLowerCase())).slice(0,30),[catalog,query]);
 const addItem=()=>{if(!selected||!item.presentationId||!Number(item.unitsPerDay)||!Number(item.treatmentDays)){setError("Selecciona medicamento, presentación, unidades por día y duración.");return;}setItems(v=>[...v,{...item,medicine:selected}]);setItem(blank);setSelected("");setPresentations([]);setError("");};
 const save=async()=>{if(!userId){setError("No se encontró el ID de tu sesión. Cierra sesión e ingresa nuevamente.");return;}if(!items.length){setError("Agrega al menos un medicamento.");return;}setSaving(true);setError("");setMessage("");
 try{const result=await fetchApi<{mensaje:string}>("/api/recetas",{method:"POST",body:JSON.stringify({patientId:userId,items:items.map(i=>({...i,presentationId:Number(i.presentationId),unitsPerDay:Number(i.unitsPerDay),treatmentDays:Number(i.treatmentDays)}))})});setMessage(result.mensaje||"Receta registrada para revisión médica.");setItems([]);await load();}
 catch(e){setError(e instanceof Error?e.message:"No se pudo registrar la receta.");}finally{setSaving(false);}};
 const grouped=useMemo(()=>{const map=new Map<number,RecipeRow[]>();recipes.forEach(r=>map.set(r.id,[...(map.get(r.id)||[]),r]));return [...map.entries()];},[recipes]);
 return <main className="space-y-6">
  <Link href="/dashboard/patient" className="inline-flex items-center gap-2 text-sm text-teal-700"><ArrowLeft size={16}/> Volver al panel</Link>
  <header><h1 className="text-2xl font-bold text-slate-900">Mis recetas y medicamentos</h1><p className="mt-1 text-sm text-slate-500">Consulta tus registros y añade una receta para que sea revisada por un profesional.</p></header>
  {error&&<p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
  {message&&<p role="status" className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
  <section className="rounded-2xl border border-slate-200 bg-white p-5 space-y-4">
   <h2 className="font-bold text-slate-900">Registrar receta / medicamentos actuales</h2>
   <p className="text-xs text-amber-800 bg-amber-50 p-3 rounded-lg">Este registro no prescribe ni valida dosis. La información queda pendiente de revisión médica; no cambies un tratamiento por lo que muestre esta plataforma.</p>
   <label className="block text-sm font-medium">Buscar medicamento del catálogo
    <div className="mt-1 flex items-center gap-2 rounded-xl border p-2"><Search size={17}/><input className="w-full outline-none" value={query} onChange={e=>setQuery(e.target.value)} placeholder="Nombre o principio activo"/></div>
   </label>
   {query&&<div className="max-h-44 overflow-auto rounded-xl border divide-y">{matches.map(m=><button type="button" key={m.medicamento} onClick={()=>{setSelected(m.medicamento);setQuery(m.medicamento);}} className="block w-full p-3 text-left text-sm hover:bg-teal-50">{m.medicamento}<span className="block text-xs text-slate-500">{m.active_ingredient||m.purpose||""}</span></button>)}{!matches.length&&<p className="p-3 text-sm text-slate-500">No hay coincidencias.</p>}</div>}
   {selected&&<div className="space-y-3 rounded-xl bg-slate-50 p-4">
    <p className="font-semibold">{selected}</p>
    <label className="block text-sm">Presentación<select className="mt-1 w-full rounded-lg border p-2" value={item.presentationId||""} onChange={e=>setItem({...item,presentationId:Number(e.target.value)})}><option value="">Selecciona presentación</option>{presentations.map(p=><option key={p.id} value={p.id}>{p.forma} · {p.concentration_value} {p.concentration_unit}{p.denominator?` / ${p.denominator}`:""} · {p.via}</option>)}</select></label>
    <div className="grid gap-3 sm:grid-cols-2">
     <label className="text-sm">Dosis indicada (según fórmula)<input className="mt-1 w-full rounded-lg border p-2" value={item.indicatedDose} onChange={e=>setItem({...item,indicatedDose:e.target.value})} placeholder="Ej. 1 tableta cada 8 horas"/></label>
     <label className="text-sm">Unidades por día<input type="number" min="0.1" step="0.1" className="mt-1 w-full rounded-lg border p-2" value={item.unitsPerDay} onChange={e=>setItem({...item,unitsPerDay:e.target.value})}/></label>
     <label className="text-sm">Unidad de consumo<input className="mt-1 w-full rounded-lg border p-2" value={item.consumptionUnit} onChange={e=>setItem({...item,consumptionUnit:e.target.value})}/></label>
     <label className="text-sm">Duración (días)<input type="number" min="1" className="mt-1 w-full rounded-lg border p-2" value={item.treatmentDays} onChange={e=>setItem({...item,treatmentDays:e.target.value})}/></label>
    </div>
    <label className="block text-sm">Indicación<input className="mt-1 w-full rounded-lg border p-2" value={item.indication} onChange={e=>setItem({...item,indication:e.target.value})} placeholder="Motivo indicado por el médico"/></label>
    <button type="button" onClick={addItem} className="inline-flex items-center gap-2 rounded-lg bg-teal-700 px-4 py-2 text-white"><Plus size={16}/> Añadir a la receta</button>
   </div>}
   {items.length>0&&<div className="space-y-2"><h3 className="font-semibold">Medicamentos por registrar</h3>{items.map((i,n)=><div key={n} className="flex items-center justify-between rounded-lg border p-3 text-sm"><span>{i.medicine} · {i.indicatedDose||"Sin dosis escrita"} · {i.treatmentDays} días</span><button onClick={()=>setItems(v=>v.filter((_,x)=>x!==n))} className="text-red-600">Quitar</button></div>)}<button disabled={saving} onClick={()=>void save()} className="rounded-xl bg-teal-700 px-5 py-3 font-semibold text-white disabled:opacity-50">{saving?"Guardando...":"Registrar y enviar a revisión"}</button></div>}
  </section>
  <section className="space-y-3"><div className="flex items-center justify-between"><h2 className="text-lg font-bold">Historial de recetas</h2><button onClick={()=>void load()} className="rounded-lg border p-2" aria-label="Actualizar"><RefreshCw size={17}/></button></div>
   {loading?<p className="text-slate-500">Cargando historial...</p>:grouped.length===0?<div className="rounded-2xl border border-dashed p-8 text-center text-slate-500">Todavía no tienes recetas registradas.</div>:grouped.map(([id,rows])=><article key={id} className="rounded-2xl border bg-white p-5"><div className="flex items-center justify-between"><h3 className="font-bold">Receta #{id}</h3><span className="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-800">{rows[0].estado_analisis}</span></div><p className="mt-1 text-xs text-slate-500">{dateLabel(rows[0].fecha_subida)}</p><div className="mt-4 space-y-3">{rows.filter(r=>r.detalle_id).map(r=><div key={r.detalle_id} className="flex gap-3 border-t pt-3"><Pill className="text-teal-700"/><div><p className="font-semibold">{r.nombre_medicamento}</p><p className="text-sm text-slate-600">{r.dosis_indicada||"Dosis no especificada"} · {r.unidades_por_dia} {r.unidad_consumo}/día · {r.dias_tratamiento} días</p>{r.indicacion&&<p className="text-xs text-slate-500">{r.indicacion}</p>}</div></div>)}</div></article>)}
  </section>
 </main>;
}
