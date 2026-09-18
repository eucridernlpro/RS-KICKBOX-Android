package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RsOpsKeysV56{
    const val MAINTENANCE="ops_maintenance_enabled_v56"
    const val MAINTENANCE_MESSAGE="ops_maintenance_message_v56"
    const val COMMUNITY_POSTS="ops_community_posts_v56"
    const val CLASS_BOOKING="ops_class_booking_v56"
    const val PRIVATE_LESSONS="ops_private_lessons_v56"
    const val REFERRALS="ops_referrals_v56"
    const val REMINDERS="ops_in_app_reminders_v56"
    const val RETENTION="ops_retention_v56"
}

fun rsOpsEnabledV56(store:RsStore,key:String,default:Boolean=true)=store.b(key,default)

private fun rsOpsUiV56(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "App Settings & Operations",
        "sub" to "Live trainer controls that change what students can do inside RS KICKBOX.",
        "live" to "LIVE STUDENT CONTROLS",
        "maintenance" to "Maintenance banner",
        "maintenance_msg" to "Maintenance message",
        "maintenance_hint" to "Students keep access, but a prominent operational notice appears at the top of the app.",
        "community" to "Allow community posting",
        "community_hint" to "Students can still read active posts when posting is disabled.",
        "booking" to "Allow new class bookings",
        "booking_hint" to "Existing bookings can still be cancelled when new booking is disabled.",
        "private" to "Allow private-lesson requests",
        "private_hint" to "Existing lesson requests can still be cancelled.",
        "referrals" to "Allow referral sharing",
        "referrals_hint" to "Referral history remains visible when sharing is disabled.",
        "reminders" to "Enable in-app reminders",
        "reminders_hint" to "Controls local reminder surfaces. Production push delivery still requires the backend.",
        "data" to "DATA & PRIVACY POLICY SETTINGS",
        "retention" to "Local retention preference",
        "retention_hint" to "This preference must match the final privacy policy before production launch.",
        "status" to "OPERATIONAL STATUS",
        "enabled" to "ENABLED",
        "disabled" to "DISABLED",
        "saved" to "Settings save immediately."
    )
    val nl=en+mapOf(
        "title" to "App-instellingen & Operaties",
        "sub" to "Live trainerinstellingen die bepalen wat leerlingen in RS KICKBOX kunnen doen.",
        "live" to "LIVE LEERLINGINSTELLINGEN",
        "maintenance" to "Onderhoudsbanner",
        "maintenance_msg" to "Onderhoudsbericht",
        "maintenance_hint" to "Leerlingen houden toegang, maar zien bovenaan de app een duidelijk operationeel bericht.",
        "community" to "Community-posts toestaan",
        "community_hint" to "Leerlingen kunnen actieve posts blijven lezen wanneer plaatsen is uitgeschakeld.",
        "booking" to "Nieuwe lesboekingen toestaan",
        "booking_hint" to "Bestaande boekingen kunnen nog steeds worden geannuleerd.",
        "private" to "Privélesverzoeken toestaan",
        "private_hint" to "Bestaande privélesverzoeken kunnen nog steeds worden geannuleerd.",
        "referrals" to "Referrals delen toestaan",
        "referrals_hint" to "Referralhistorie blijft zichtbaar als delen uitstaat.",
        "reminders" to "In-app herinneringen inschakelen",
        "reminders_hint" to "Beheert lokale herinneringen. Productie-push vereist later de backend.",
        "data" to "DATA & PRIVACYBELEID",
        "retention" to "Lokale bewaartermijn",
        "retention_hint" to "Deze voorkeur moet bij release overeenkomen met het definitieve privacybeleid.",
        "status" to "OPERATIONELE STATUS",
        "enabled" to "INGESCHAKELD",
        "disabled" to "UITGESCHAKELD",
        "saved" to "Instellingen worden direct opgeslagen."
    )
    val pt=en+mapOf(
        "title" to "Definições & Operações",
        "sub" to "Controlos do treinador que alteram o que os alunos podem fazer na app.",
        "live" to "CONTROLOS LIVE DOS ALUNOS",
        "maintenance" to "Banner de manutenção",
        "maintenance_msg" to "Mensagem de manutenção",
        "community" to "Permitir publicações na comunidade",
        "booking" to "Permitir novas reservas de aulas",
        "private" to "Permitir pedidos de aulas privadas",
        "referrals" to "Permitir partilha de referências",
        "reminders" to "Ativar lembretes na app",
        "data" to "DADOS & PRIVACIDADE",
        "retention" to "Preferência de retenção local",
        "status" to "ESTADO OPERACIONAL",
        "enabled" to "ATIVO",
        "disabled" to "DESATIVADO",
        "saved" to "As definições são guardadas imediatamente."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsOpsToggleV56(
    c:RsPalette,
    title:String,
    hint:String,
    value:Boolean,
    onChange:(Boolean)->Unit
){
    RsPanel(c){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(title,color=c.text,fontWeight=FontWeight.Bold)
                Text(hint,color=c.muted,fontSize=10.sp)
            }
            Switch(value,onChange)
        }
    }
}

@Composable
fun RsAdminSettingsV56(c:RsPalette,store:RsStore,lang:RsLang){
    var maintenance by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.MAINTENANCE,false))}
    var maintenanceMessage by remember{
        mutableStateOf(store.s(RsOpsKeysV56.MAINTENANCE_MESSAGE,"RS KICKBOX maintenance notice: some services may be temporarily limited."))
    }
    var community by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true))}
    var booking by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.CLASS_BOOKING,true))}
    var privateLessons by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.PRIVATE_LESSONS,true))}
    var referrals by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REFERRALS,true))}
    var reminders by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REMINDERS,true))}
    var retention by remember{mutableStateOf(store.s(RsOpsKeysV56.RETENTION,"24 months"))}

    fun setBool(key:String,value:Boolean){store.pb(key,value)}

    RsScroll(c,rsOpsUiV56(lang,"title"),rsOpsUiV56(lang,"sub")){
        Text(rsOpsUiV56(lang,"live"),color=c.bright,fontWeight=FontWeight.Black)

        RsOpsToggleV56(
            c,rsOpsUiV56(lang,"maintenance"),rsOpsUiV56(lang,"maintenance_hint"),maintenance
        ){v->maintenance=v;setBool(RsOpsKeysV56.MAINTENANCE,v)}

        if(maintenance)RsPanel(c){
            OutlinedTextField(
                value=maintenanceMessage,
                onValueChange={
                    maintenanceMessage=it.take(240)
                    store.ps(RsOpsKeysV56.MAINTENANCE_MESSAGE,maintenanceMessage)
                },
                label={Text(rsOpsUiV56(lang,"maintenance_msg"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2
            )
        }

        RsOpsToggleV56(c,rsOpsUiV56(lang,"community"),rsOpsUiV56(lang,"community_hint"),community){
            community=it;setBool(RsOpsKeysV56.COMMUNITY_POSTS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"booking"),rsOpsUiV56(lang,"booking_hint"),booking){
            booking=it;setBool(RsOpsKeysV56.CLASS_BOOKING,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"private"),rsOpsUiV56(lang,"private_hint"),privateLessons){
            privateLessons=it;setBool(RsOpsKeysV56.PRIVATE_LESSONS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"referrals"),rsOpsUiV56(lang,"referrals_hint"),referrals){
            referrals=it;setBool(RsOpsKeysV56.REFERRALS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"reminders"),rsOpsUiV56(lang,"reminders_hint"),reminders){
            reminders=it;setBool(RsOpsKeysV56.REMINDERS,it)
        }

        Text(rsOpsUiV56(lang,"data"),color=c.bright,fontWeight=FontWeight.Black)
        RsPanel(c){
            Text(rsOpsUiV56(lang,"retention"),color=c.text,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("12 months","24 months","36 months").forEach{x->
                    FilterChip(
                        selected=retention==x,
                        onClick={retention=x;store.ps(RsOpsKeysV56.RETENTION,x)},
                        label={Text(x,fontSize=9.sp)},
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            Text(rsOpsUiV56(lang,"retention_hint"),color=c.muted,fontSize=10.sp)
        }

        Text(rsOpsUiV56(lang,"status"),color=c.bright,fontWeight=FontWeight.Black)
        RsPanel(c){
            listOf(
                rsOpsUiV56(lang,"community") to community,
                rsOpsUiV56(lang,"booking") to booking,
                rsOpsUiV56(lang,"private") to privateLessons,
                rsOpsUiV56(lang,"referrals") to referrals,
                rsOpsUiV56(lang,"reminders") to reminders
            ).forEach{(name,on)->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(name,color=c.text,fontSize=10.sp,modifier=Modifier.weight(1f))
                    Text(if(on)rsOpsUiV56(lang,"enabled") else rsOpsUiV56(lang,"disabled"),color=if(on)c.bright else c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                }
            }
            Text(rsOpsUiV56(lang,"saved"),color=c.muted,fontSize=9.sp)
        }
    }
}
