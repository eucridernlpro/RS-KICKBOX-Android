package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RsPlanV49(
    val code:String,val name:String,val monthlyCents:Int,val active:Boolean,val description:String
)

private fun rsSeedPlansV49()=listOf(
    RsPlanV49("BASIC","RS BASIC",2900,true,"Core training library, classes and member access."),
    RsPlanV49("PRO","RS PRO",4900,true,"Expanded coaching, advanced content and full member tools."),
    RsPlanV49("ELITE","RS ELITE",6900,true,"Premium access for advanced coaching and exclusive content.")
)

fun rsLoadPlansV49(store:RsStore):List<RsPlanV49>{
    val raw=store.s("membership_plans_v49","")
    if(raw.isBlank()){
        rsSavePlansV49(store,rsSeedPlansV49())
        return rsSeedPlansV49()
    }
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsPlanV49(
                    o.optString("code"),o.optString("name"),
                    o.optInt("monthlyCents",0).coerceAtLeast(0),
                    o.optBoolean("active",true),o.optString("description")
                ))
            }
        }
    }.getOrDefault(rsSeedPlansV49())
}
fun rsSavePlansV49(store:RsStore,items:List<RsPlanV49>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("code",x.code);put("name",x.name);put("monthlyCents",x.monthlyCents)
        put("active",x.active);put("description",x.description)
    })}
    store.ps("membership_plans_v49",a.toString())
}

fun rsPlanMonthlyCentsV49(store:RsStore,code:String):Int =
    rsLoadPlansV49(store).firstOrNull{it.code.equals(code,true)}?.monthlyCents
        ?:when(code.uppercase()){"BASIC"->2900;"ELITE"->6900;else->4900}

private fun rsPlanUiV49(lang:RsLang,key:String):String{
    val en=mapOf(
        "plans" to "Membership Plans","plans_sub" to "Control plan pricing, availability and descriptions.",
        "access" to "Access & Subscriptions","access_sub" to "Assign student plans and activate or deactivate access.",
        "price" to "Monthly price €","description" to "Description","active" to "ACTIVE","inactive" to "INACTIVE",
        "save" to "Save plans","student" to "Student","current" to "Current plan","change" to "Change plan",
        "demo_note" to "The built-in Alex preview account remains available for acceptance testing."
    )
    val nl=en+mapOf("plans" to "Lidmaatschapsplannen","plans_sub" to "Beheer prijzen, beschikbaarheid en beschrijvingen.","access" to "Toegang & Abonnementen","access_sub" to "Wijs plannen toe en activeer/deactiveer leerlingtoegang.","price" to "Maandprijs €","description" to "Beschrijving","active" to "ACTIEF","inactive" to "INACTIEF","save" to "Plannen opslaan","student" to "Leerling","current" to "Huidig plan","change" to "Plan wijzigen","demo_note" to "Het ingebouwde Alex-previewaccount blijft beschikbaar voor acceptatietests.")
    val pt=en+mapOf("plans" to "Planos de Adesão","plans_sub" to "Controla preços, disponibilidade e descrições dos planos.","access" to "Acesso & Subscrições","access_sub" to "Atribui planos e ativa ou desativa o acesso dos alunos.","price" to "Preço mensal €","description" to "Descrição","active" to "ATIVO","inactive" to "INATIVO","save" to "Guardar planos","student" to "Aluno","current" to "Plano atual","change" to "Alterar plano","demo_note" to "A conta de prévia Alex continua disponível para testes de aceitação.")
    val es=en+mapOf("plans" to "Planes de Membresía","plans_sub" to "Controla precios, disponibilidad y descripciones de los planes.","access" to "Acceso & Suscripciones","access_sub" to "Asigna planes y activa o desactiva el acceso de alumnos.","price" to "Precio mensual €","description" to "Descripción","active" to "ACTIVO","inactive" to "INACTIVO","save" to "Guardar planes","student" to "Alumno","current" to "Plan actual","change" to "Cambiar plan","demo_note" to "La cuenta de prueba Alex sigue disponible para tests de aceptación.")
    val fr=en+mapOf("plans" to "Formules d’Adhésion","plans_sub" to "Gère les prix, la disponibilité et les descriptions.","access" to "Accès & Abonnements","access_sub" to "Attribue les formules et active ou désactive l’accès des élèves.","price" to "Prix mensuel €","description" to "Description","active" to "ACTIF","inactive" to "INACTIF","save" to "Enregistrer les formules","student" to "Élève","current" to "Formule actuelle","change" to "Changer de formule","demo_note" to "Le compte de prévisualisation Alex reste disponible pour les tests d’acceptation.")
    val de=en+mapOf("plans" to "Mitgliedschaftspläne","plans_sub" to "Steuere Preise, Verfügbarkeit und Beschreibungen.","access" to "Zugriff & Abos","access_sub" to "Weise Schülerpläne zu und aktiviere oder deaktiviere Zugriff.","price" to "Monatspreis €","description" to "Beschreibung","active" to "AKTIV","inactive" to "INAKTIV","save" to "Pläne speichern","student" to "Schüler","current" to "Aktueller Plan","change" to "Plan ändern","demo_note" to "Das integrierte Alex-Vorschaukonto bleibt für Akzeptanztests verfügbar.")
    val it=en+mapOf("plans" to "Piani Abbonamento","plans_sub" to "Gestisci prezzi, disponibilità e descrizioni dei piani.","access" to "Accesso & Abbonamenti","access_sub" to "Assegna piani e attiva o disattiva l’accesso degli allievi.","price" to "Prezzo mensile €","description" to "Descrizione","active" to "ATTIVO","inactive" to "INATTIVO","save" to "Salva piani","student" to "Allievo","current" to "Piano attuale","change" to "Cambia piano","demo_note" to "L’account di anteprima Alex resta disponibile per i test di accettazione.")
    val pl=en+mapOf("plans" to "Plany Członkostwa","plans_sub" to "Zarządzaj cenami, dostępnością i opisami planów.","access" to "Dostęp & Subskrypcje","access_sub" to "Przydzielaj plany i włączaj lub wyłączaj dostęp uczniów.","price" to "Cena miesięczna €","description" to "Opis","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","save" to "Zapisz plany","student" to "Uczeń","current" to "Aktualny plan","change" to "Zmień plan","demo_note" to "Wbudowane konto podglądowe Alex pozostaje dostępne do testów akceptacyjnych.")
    val tr=en+mapOf("plans" to "Üyelik Planları","plans_sub" to "Plan fiyatlarını, kullanılabilirliği ve açıklamaları yönet.","access" to "Erişim & Abonelikler","access_sub" to "Öğrenci planlarını ata ve erişimi etkinleştir veya devre dışı bırak.","price" to "Aylık fiyat €","description" to "Açıklama","active" to "AKTİF","inactive" to "PASİF","save" to "Planları kaydet","student" to "Öğrenci","current" to "Mevcut plan","change" to "Planı değiştir","demo_note" to "Yerleşik Alex önizleme hesabı kabul testleri için kullanılabilir durumda kalır.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsMembershipPlansV49(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val plans=remember(revision){rsLoadPlansV49(store)}
    var edited by remember(revision){mutableStateOf(plans)}

    RsScroll(c,rsPlanUiV49(lang,"plans"),rsPlanUiV49(lang,"plans_sub")){
        edited.forEachIndexed{index,plan->
            RsPanel(c){
                Text(plan.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
                OutlinedTextField(
                    value=(plan.monthlyCents/100.0).toString(),
                    onValueChange={raw->
                        val cents=((raw.replace(',','.').toDoubleOrNull()?:0.0)*100).toInt().coerceAtLeast(0)
                        edited=edited.toMutableList().also{it[index]=plan.copy(monthlyCents=cents)}
                    },
                    label={Text(rsPlanUiV49(lang,"price"))},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true
                )
                OutlinedTextField(
                    value=plan.description,
                    onValueChange={v->edited=edited.toMutableList().also{it[index]=plan.copy(description=v.take(500))}},
                    label={Text(rsPlanUiV49(lang,"description"))},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(plan.active)rsPlanUiV49(lang,"active") else rsPlanUiV49(lang,"inactive"),color=c.muted)
                    Switch(plan.active,{v->edited=edited.toMutableList().also{it[index]=plan.copy(active=v)}})
                }
            }
        }
        Button(
            onClick={rsSavePlansV49(store,edited);revision++},
            modifier=Modifier.fillMaxWidth()
        ){Text(rsPlanUiV49(lang,"save"))}
    }
}

@Composable
fun RsAccessControlV49(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val students=remember(revision){rsLoadStudentsV33(store)}
    val plans=rsLoadPlansV49(store).filter{it.active}
    RsScroll(c,rsPlanUiV49(lang,"access"),rsPlanUiV49(lang,"access_sub")){
        Text(rsPlanUiV49(lang,"demo_note"),color=c.muted,fontSize=10.sp)
        if(students.isEmpty())RsPanel(c){Text("No trainer-created student accounts yet.",color=c.muted)}
        students.forEach{student->
            RsPanel(c){
                Text(student.name.ifBlank{student.email},color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(student.email,color=c.muted,fontSize=10.sp)
                Text(rsPlanUiV49(lang,"current")+" · "+student.plan,color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    plans.forEach{plan->
                        FilterChip(
                            selected=student.plan.equals(plan.code,true),
                            onClick={
                                rsSaveStudentsV33(store,rsLoadStudentsV33(store).map{
                                    if(it.id==student.id)it.copy(plan=plan.code) else it
                                })
                                revision++
                            },
                            label={Text(plan.code,fontSize=9.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(student.active)rsPlanUiV49(lang,"active") else rsPlanUiV49(lang,"inactive"),color=c.muted)
                    Switch(student.active,{v->
                        rsSaveStudentsV33(store,rsLoadStudentsV33(store).map{
                            if(it.id==student.id)it.copy(active=v) else it
                        })
                        revision++
                    })
                }
            }
        }
    }
}
