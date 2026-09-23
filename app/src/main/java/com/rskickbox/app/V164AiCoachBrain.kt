package com.rskickbox.app

import android.content.Context
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject

data class RsAiCoachReferenceV164(
    val item:RsTrainingMediaItemV55,
    val localUri:String
)

private fun rsAiGreetingV164(q:String):Boolean{
    val s=q.trim().lowercase()
    if(s.length>70)return false
    return listOf(
        "hi","hello","hey","good morning","good afternoon","good evening",
        "hoi","hallo","goedemorgen","goedemiddag","goedenavond",
        "olá","ola","bom dia","boa tarde","boa noite",
        "hola","buenos días","buenos dias","buenas tardes","buenas noches",
        "bonjour","bonsoir","salut",
        "hallo","guten morgen","guten abend",
        "ciao","buongiorno","dobry","merhaba"
    ).any{term->s==term || s.startsWith(term+" ")}
}

private fun rsAiThanksV164(q:String):Boolean{
    val s=q.lowercase()
    return listOf("thank","thanks","dank","bedankt","obrigad","gracias","merci","danke","grazie","teşekkür","dzieku").any{s.contains(it)}
}

private fun rsAiTrainingIntentV164(q:String):Boolean{
    val s=q.lowercase()
    return listOf(
        "kick","punch","jab","cross","hook","uppercut","knee","elbow",
        "roundhouse","low kick","high kick","front kick","teep","push kick",
        "combination","combo","guard","stance","footwork","pivot","sparring",
        "bag","pads","padwork","conditioning","training","drill","defence","defense",
        "block","check","counter","clinch","distance","timing","speed","power",
        "kickbox","muay thai","dutch style","boxing","cardio","fight camp",
        "trap","stoot","hoek","knie","combinatie","dekking","houding","training",
        "chute","soco","joelho","combinação","treino","golpe",
        "patada","golpe","rodilla","combinación","entrenamiento",
        "coup","genou","combinaison","entraînement"
    ).any{s.contains(it)}
}

fun rsAiLocalCoachAnswerV164(code:String,question:String):String{
    val q=question.trim()
    val lower=q.lowercase()

    if(q.isBlank())return when(code){
        "nl"->"Hoi! Ik help je met de RS-app, muziek, pagina’s en kickbokstraining. Wat wil je doen?"
        "pt"->"Pergunta-me sobre kickboxing, uma técnica, combinação ou treino. Também podes carregar um vídeo."
        "es"->"Pregúntame sobre kickboxing, una técnica, combinación o entrenamiento. También puedes subir un vídeo."
        "fr"->"Pose-moi une question sur le kickboxing, une technique, combinaison ou séance. Tu peux aussi envoyer une vidéo."
        else->"Hi! I can help with RS pages, music and kickboxing training. What would you like to do?"
    }

    if(rsAiGreetingV164(q))return when(code){
        "nl"->"Hoi! Goed je te zien. Ik ben je RS-assistent. Waarmee kan ik je vandaag helpen?"
        "pt"->"Olá! Bom ter-te aqui. Sou o teu treinador IA RS. Pergunta sobre uma técnica, combinação, tipo de treino ou envia um vídeo para analisarmos algo específico."
        "es"->"¡Hola! Me alegra verte. Soy tu entrenador IA RS. Pregúntame por una técnica, combinación, tipo de entrenamiento o sube un vídeo para revisar algo concreto."
        "fr"->"Salut ! Heureux de te voir. Je suis ton coach IA RS. Demande-moi une technique, une combinaison, un type d’entraînement ou envoie une vidéo."
        "de"->"Hallo! Schön, dass du da bist. Ich bin dein RS KI-Trainer. Frag mich nach Technik, Kombinationen, Trainingsformen oder lade ein Video hoch."
        "it"->"Ciao! Sono il tuo allenatore IA RS. Chiedimi una tecnica, combinazione, tipo di allenamento oppure carica un video."
        "pl"->"Cześć! Jestem trenerem AI RS. Zapytaj o technikę, kombinację, trening albo prześlij film."
        "tr"->"Merhaba! Ben RS YZ antrenörünüm. Teknik, kombinasyon veya antrenman hakkında sorabilir ya da video yükleyebilirsin."
        else->"Hi! Good to see you. I’m your RS assistant. What can I help you with today?"
    }

    if(rsAiThanksV164(q))return when(code){
        "nl"->"Graag gedaan. Als je wilt, kunnen we meteen doorgaan met een combinatie, techniek of trainingsvideo."
        "pt"->"De nada. Se quiseres, podemos continuar com uma combinação, técnica ou vídeo de treino."
        "es"->"De nada. Si quieres, seguimos con una combinación, técnica o vídeo de entrenamiento."
        "fr"->"Avec plaisir. On peut continuer avec une combinaison, une technique ou une vidéo d’entraînement."
        else->"You’re welcome. We can continue with a combination, technique or training video whenever you’re ready."
    }

    fun base(move:String,why:String,steps:String,mistakes:String,drill:String)=when(code){
        "nl"->"$move — $why\n\nTechniek: $steps\n\nLet op: $mistakes\n\nDrill: $drill"
        "pt"->"$move — $why\n\nTécnica: $steps\n\nAtenção: $mistakes\n\nExercício: $drill"
        "es"->"$move — $why\n\nTécnica: $steps\n\nAtención: $mistakes\n\nEjercicio: $drill"
        "fr"->"$move — $why\n\nTechnique : $steps\n\nAttention : $mistakes\n\nExercice : $drill"
        else->"$move — $why\n\nTechnique: $steps\n\nWatch for: $mistakes\n\nDrill: $drill"
    }

    return when{
        lower.contains("roundhouse")||lower.contains("round kick")||lower.contains("ronde trap")||lower.contains("chute circular")->base(
            "Roundhouse kick",
            if(code=="nl")"een krachtige rotatietrap die snelheid uit heuprotatie en steunvoet haalt." else "a rotational kick that gets speed and power from the hip and supporting-foot pivot.",
            if(code=="nl")"stap of pivot op de steunvoet, draai heup en schouder samen, houd de tegenovergestelde hand hoog en trek het been gecontroleerd terug." else "pivot the supporting foot, turn hip and shoulder together, keep the opposite hand high, then recover the leg under control.",
            if(code=="nl")"te weinig pivot, achterover leunen, de dekking laten zakken of het been na impact laten hangen." else "too little pivot, leaning back excessively, dropping the guard, or failing to recover after impact.",
            if(code=="nl")"3×10 technisch langzaam op de zak, daarna 3×6 sneller met volledige terugkeer naar houding." else "3×10 slow technical reps on the bag, then 3×6 faster reps with full return to stance."
        )
        lower.contains("low kick")->base(
            "Low kick",
            if(code=="nl")"een effectieve aanval op het been om balans, mobiliteit en ritme van de tegenstander te verstoren." else "an effective leg attack used to disrupt balance, mobility and rhythm.",
            if(code=="nl")"zet afstand goed, pivot, draai de heup door het doel, raak met het scheenbeen en kom direct terug in dekking." else "set the range, pivot, rotate the hip through the target, connect with the shin, then return directly to guard.",
            if(code=="nl")"alleen met het onderbeen zwaaien, tenen laten wijzen zonder heuprotatie of de handen laten zakken." else "swinging only the lower leg, missing the hip rotation, or dropping the hands.",
            if(code=="nl")"jab-cross-low-kick op de zak: 5 rondes van 1 minuut technisch, 30 seconden rust." else "jab-cross-low-kick on the bag: 5 technical 1-minute rounds with 30 seconds rest."
        )
        lower.contains("jab")&&lower.contains("cross")->base(
            "Jab–cross",
            if(code=="nl")"de basiscombinatie voor afstand, timing en het openen van vervolgtechnieken." else "the core boxing combination for range, timing and opening follow-up attacks.",
            if(code=="nl")"jab recht vanuit de dekking, cross met rotatie vanuit voet-heup-schouder, kin laag en beide handen snel terug." else "send the jab straight from guard, rotate foot-hip-shoulder into the cross, keep the chin protected and recover both hands quickly.",
            if(code=="nl")"armen duwen zonder rotatie, te breed staan, kin omhoog of niet terugkeren naar dekking." else "pushing with the arms, overreaching, lifting the chin or failing to return to guard.",
            if(code=="nl")"2 minuten shadowboxing: jab-cross, stap uit, reset. Daarna dezelfde combinatie op pads of zak." else "2 minutes shadowboxing: jab-cross, angle out, reset. Then repeat on pads or bag."
        )
        lower.contains("guard")||lower.contains("dekking")->base(
            if(code=="nl")"Dekking" else "Guard",
            if(code=="nl")"je basis voor verdediging én voor sneller terugkomen na elke aanval." else "your defensive base and the position that lets you recover quickly after every attack.",
            if(code=="nl")"kin licht omlaag, handen dichtbij het hoofd, ellebogen gecontroleerd, schouders ontspannen en ogen vooruit." else "chin slightly down, hands close to the head, elbows controlled, shoulders relaxed and eyes forward.",
            if(code=="nl")"handen te ver van het gezicht, ellebogen wijd, verstijven of na een combinatie open blijven staan." else "hands drifting away from the face, elbows flaring, excessive tension, or staying open after a combination.",
            if(code=="nl")"shadowbox 3 rondes waarbij elke techniek bewust eindigt in dezelfde compacte dekking." else "shadowbox 3 rounds where every technique deliberately finishes back in the same compact guard."
        )
        lower.contains("dutch")||lower.contains("muay thai")||lower.contains("style")->when(code){
            "nl"->"Kickboksstijlen verschillen vooral in ritme, afstand en combinaties. Nederlandse kickboksstijl staat bekend om harde bokscombinaties die vaak eindigen met low kicks. Muay Thai gebruikt doorgaans meer clinch, knieën, ellebogen en een ander ritme. Een boksgerichte stijl gebruikt meer hoofdbeweging, hoeken en handcombinaties. De beste keuze hangt af van je regels, lichaamstype en trainingsdoel. Vraag me bijvoorbeeld: ‘geef me een Nederlandse 4-techniek combinatie’."
            "pt"->"Os estilos de kickboxing diferem sobretudo no ritmo, distância e combinações. O estilo holandês costuma ligar combinações fortes de boxe a low kicks. Muay Thai usa mais clinch, joelhos, cotovelos e outro ritmo. Podes pedir-me, por exemplo, uma combinação holandesa de quatro técnicas."
            else->"Kickboxing styles mainly differ in rhythm, range and preferred combinations. Dutch-style kickboxing is known for heavy boxing combinations that often finish with low kicks. Muay Thai typically uses more clinch, knees, elbows and a different rhythm. Boxing-heavy kickboxing uses more hand combinations, angles and head movement. Ask me for a specific style or combination and I’ll break it down."
        }
        lower.contains("training")||lower.contains("treino")||lower.contains("entrenamiento")||lower.contains("drill")->when(code){
            "nl"->"Voor een complete kickbokstraining kun je werken in blokken: 8–10 min warming-up, 10 min techniek, 10–15 min combinaties op pads/zak, 10 min defensie of voetenwerk, 3–5 gecontroleerde rondes conditioneel werk en een korte cooling-down. Vertel me je niveau, beschikbare tijd en doel, dan maak ik een concreet schema."
            "pt"->"Para um treino completo: 8–10 min aquecimento, 10 min técnica, 10–15 min combinações em pads/saco, 10 min defesa ou deslocação, 3–5 rounds de condicionamento controlado e recuperação final. Diz-me o teu nível, tempo disponível e objetivo para eu montar uma sessão."
            else->"For a complete kickboxing session, use blocks: 8–10 min warm-up, 10 min technique, 10–15 min combinations on pads/bag, 10 min defense or footwork, 3–5 controlled conditioning rounds, then a short cool-down. Tell me your level, available time and goal and I can build the session."
        }
        rsAiTrainingIntentV164(q)->when(code){
            "nl"->"Ik kan dit technisch met je uitwerken. Geef me de exacte techniek of combinatie en je doel — snelheid, kracht, balans, timing, verdediging of conditie. Als je een video uploadt, kan ik de zichtbare houding, dekking, rotatie en herstelpositie specifieker beoordelen."
            "pt"->"Posso trabalhar isto tecnicamente contigo. Diz a técnica ou combinação exata e o objetivo — velocidade, potência, equilíbrio, timing, defesa ou condição. Se enviares um vídeo, posso analisar melhor postura, guarda, rotação e recuperação."
            else->"I can break this down technically. Tell me the exact move or combination and your goal—speed, power, balance, timing, defense or conditioning. If you upload a video, I can give more specific feedback on visible stance, guard, rotation and recovery."
        }
        else->when(code){
            "nl"->"Ik ben je RS-assistent én kickbokscoach. Ik kan met je praten, je door RS KICKBOXING sturen, pagina’s openen, muziek bedienen of toevoegen, instellingen helpen wijzigen en je natuurlijk ook helpen met training, technieken en combinaties. Zeg gewoon wat je wilt doen."
            "pt"->"Sou o teu assistente RS e também treinador de kickboxing. Posso conversar contigo, navegar pela app, abrir páginas, controlar ou adicionar música, ajudar nas definições e também orientar treinos, técnicas e combinações. Diz apenas o que queres fazer."
            "es"->"Soy tu asistente RS y también entrenador de kickboxing. Puedo hablar contigo, moverme por la app, abrir páginas, controlar o añadir música, ayudarte con ajustes y también con entrenamientos, técnicas y combinaciones. Solo dime qué quieres hacer."
            "fr"->"Je suis ton assistant RS et aussi ton coach kickboxing. Je peux discuter avec toi, naviguer dans l’app, ouvrir des pages, contrôler ou ajouter de la musique, t’aider avec les réglages et bien sûr avec l’entraînement. Dis-moi simplement ce que tu veux faire."
            "de"->"Ich bin dein RS-Assistent und Kickbox-Trainer. Ich kann mit dir sprechen, durch die App navigieren, Seiten öffnen, Musik steuern oder hinzufügen, Einstellungen ändern und dich beim Training unterstützen. Sag einfach, was du tun möchtest."
            else->"I’m your RS assistant as well as your kickboxing coach. I can talk with you naturally, move through RS KICKBOXING, open pages, control or add music, help with settings, and coach training, techniques and combinations. Just tell me what you want to do."
        }
    }
}

suspend fun rsOnlineAiCoachV164(
    question:String,
    lang:RsLang,
    referenceSummary:String
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val response=client.functions.invoke(
        function="ai-coach-chat",
        body=buildJsonObject{
            put("question",question.take(1800))
            put("language",lang.name)
            put("references",referenceSummary.take(3500))
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val msg=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
        error(msg.ifBlank{"RS AI Coach is temporarily unavailable."})
    }
    val answer=JSONObject(raw).optString("answer").trim()
    require(answer.isNotBlank()){"RS AI Coach returned an empty answer."}
    answer
}

suspend fun rsAiReferencePreviewsV164(
    context:Context,
    question:String,
    answer:String
):List<RsAiCoachReferenceV164>{
    val matched=rsAiReferenceMatchesV114(question,answer).getOrDefault(emptyList()).take(2)
    return matched.mapNotNull{item->
        rsCloudTrainingMediaLocalUriV73(context,item).getOrNull()?.let{local->
            RsAiCoachReferenceV164(item,local)
        }
    }
}
