package com.rskickbox.app

fun rsAiUiV162(code:String,key:String):String{
    val en=mapOf(
        "title" to "RS AI TRAINER",
        "coaching" to "kickboxing coaching",
        "voice_language" to "VOICE LANGUAGE",
        "desc" to "Ask by text or voice, analyze technique video, and hear the coach answer in the selected language.",
        "ask_title" to "ASK YOUR COACH",
        "question" to "Kickboxing question",
        "ask_voice" to "Ask with voice",
        "upload_video" to "Upload video",
        "ask_button" to "Ask RS AI Trainer",
        "speak" to "Speak answer",
        "analysis" to "TECHNIQUE ANALYSIS",
        "analysis_desc" to "Upload a short technique video, review correction points and use trainer-approved reference media.",
        "open_analysis" to "Open Technique Analysis",
        "voice_ready" to "Voice question ready.",
        "voice_unavailable" to "Voice input is not available on this device."
    )
    val nl=en+mapOf(
        "title" to "RS AI TRAINER","coaching" to "kickbokscoaching","voice_language" to "STEMTAAL",
        "desc" to "Vraag via tekst of stem, analyseer techniekvideo en hoor de coach antwoorden in de gekozen taal.",
        "ask_title" to "VRAAG JE COACH","question" to "Kickboksvraag","ask_voice" to "Vraag met stem",
        "upload_video" to "Video uploaden","ask_button" to "Vraag RS AI Trainer","speak" to "Antwoord uitspreken",
        "analysis" to "TECHNIEKANALYSE","analysis_desc" to "Upload een korte techniekvideo, bekijk correctiepunten en gebruik door de trainer goedgekeurde voorbeelden.",
        "open_analysis" to "Open Techniekanalyse","voice_ready" to "Stemvraag klaar.","voice_unavailable" to "Steminvoer is niet beschikbaar op dit apparaat."
    )
    val pt=en+mapOf(
        "title" to "TREINADOR IA RS","coaching" to "treino de kickboxing","voice_language" to "IDIOMA DA VOZ",
        "desc" to "Pergunta por texto ou voz, analisa vídeo técnico e ouve o treinador responder no idioma escolhido.",
        "ask_title" to "PERGUNTA AO TREINADOR","question" to "Pergunta de kickboxing","ask_voice" to "Perguntar por voz",
        "upload_video" to "Carregar vídeo","ask_button" to "Perguntar ao Treinador IA RS","speak" to "Ouvir resposta",
        "analysis" to "ANÁLISE TÉCNICA","analysis_desc" to "Carrega um vídeo técnico curto, revê correções e usa referências aprovadas pelo treinador.",
        "open_analysis" to "Abrir análise técnica","voice_ready" to "Pergunta de voz pronta.","voice_unavailable" to "A entrada de voz não está disponível neste dispositivo."
    )
    val es=en+mapOf(
        "title" to "ENTRENADOR IA RS","coaching" to "entrenamiento de kickboxing","voice_language" to "IDIOMA DE VOZ",
        "desc" to "Pregunta por texto o voz, analiza vídeo técnico y escucha al entrenador responder en el idioma seleccionado.",
        "ask_title" to "PREGUNTA A TU ENTRENADOR","question" to "Pregunta de kickboxing","ask_voice" to "Preguntar por voz",
        "upload_video" to "Subir vídeo","ask_button" to "Preguntar al Entrenador IA RS","speak" to "Reproducir respuesta",
        "analysis" to "ANÁLISIS TÉCNICO","analysis_desc" to "Sube un vídeo técnico corto, revisa correcciones y usa referencias aprobadas por el entrenador.",
        "open_analysis" to "Abrir análisis técnico","voice_ready" to "Pregunta de voz lista.","voice_unavailable" to "La entrada de voz no está disponible en este dispositivo."
    )
    val fr=en+mapOf(
        "title" to "COACH IA RS","coaching" to "coaching kickboxing","voice_language" to "LANGUE VOCALE",
        "desc" to "Pose une question par texte ou voix, analyse une vidéo technique et écoute la réponse du coach dans la langue choisie.",
        "ask_title" to "DEMANDE À TON COACH","question" to "Question de kickboxing","ask_voice" to "Question vocale",
        "upload_video" to "Importer une vidéo","ask_button" to "Demander au Coach IA RS","speak" to "Lire la réponse",
        "analysis" to "ANALYSE TECHNIQUE","analysis_desc" to "Importe une courte vidéo technique, examine les corrections et utilise les références approuvées par le coach.",
        "open_analysis" to "Ouvrir l’analyse technique","voice_ready" to "Question vocale prête.","voice_unavailable" to "La saisie vocale n’est pas disponible sur cet appareil."
    )
    val de=en+mapOf(
        "title" to "RS KI-TRAINER","coaching" to "Kickbox-Coaching","voice_language" to "SPRACHSPRACHE",
        "desc" to "Frage per Text oder Sprache, analysiere Technikvideos und höre die Antwort des Trainers in der gewählten Sprache.",
        "ask_title" to "FRAGE DEINEN TRAINER","question" to "Kickbox-Frage","ask_voice" to "Per Sprache fragen",
        "upload_video" to "Video hochladen","ask_button" to "RS KI-Trainer fragen","speak" to "Antwort vorlesen",
        "analysis" to "TECHNIKANALYSE","analysis_desc" to "Lade ein kurzes Technikvideo hoch, prüfe Korrekturpunkte und nutze trainergeprüfte Referenzen.",
        "open_analysis" to "Technikanalyse öffnen","voice_ready" to "Sprachfrage bereit.","voice_unavailable" to "Spracheingabe ist auf diesem Gerät nicht verfügbar."
    )
    val it=en+mapOf(
        "title" to "ALLENATORE IA RS","coaching" to "coaching kickboxing","voice_language" to "LINGUA VOCALE",
        "desc" to "Chiedi con testo o voce, analizza video tecnici e ascolta la risposta dell’allenatore nella lingua scelta.",
        "ask_title" to "CHIEDI AL TUO ALLENATORE","question" to "Domanda di kickboxing","ask_voice" to "Chiedi con la voce",
        "upload_video" to "Carica video","ask_button" to "Chiedi all’Allenatore IA RS","speak" to "Leggi risposta",
        "analysis" to "ANALISI TECNICA","analysis_desc" to "Carica un breve video tecnico, rivedi le correzioni e usa riferimenti approvati dal trainer.",
        "open_analysis" to "Apri analisi tecnica","voice_ready" to "Domanda vocale pronta.","voice_unavailable" to "L’input vocale non è disponibile su questo dispositivo."
    )
    val pl=en+mapOf(
        "title" to "TRENER AI RS","coaching" to "trening kickboxingu","voice_language" to "JĘZYK GŁOSU",
        "desc" to "Pytaj tekstem lub głosem, analizuj filmy techniczne i słuchaj odpowiedzi trenera w wybranym języku.",
        "ask_title" to "ZAPYTAJ TRENERA","question" to "Pytanie o kickboxing","ask_voice" to "Zapytaj głosem",
        "upload_video" to "Prześlij wideo","ask_button" to "Zapytaj Trenera AI RS","speak" to "Odtwórz odpowiedź",
        "analysis" to "ANALIZA TECHNIKI","analysis_desc" to "Prześlij krótki film techniczny, sprawdź korekty i korzystaj z referencji zatwierdzonych przez trenera.",
        "open_analysis" to "Otwórz analizę techniki","voice_ready" to "Pytanie głosowe gotowe.","voice_unavailable" to "Wprowadzanie głosowe nie jest dostępne na tym urządzeniu."
    )
    val tr=en+mapOf(
        "title" to "RS YZ ANTRENÖR","coaching" to "kickboks koçluğu","voice_language" to "SES DİLİ",
        "desc" to "Metin veya sesle sor, teknik videoyu analiz et ve antrenörün seçilen dilde yanıtını dinle.",
        "ask_title" to "ANTRENÖRÜNE SOR","question" to "Kickboks sorusu","ask_voice" to "Sesle sor",
        "upload_video" to "Video yükle","ask_button" to "RS YZ Antrenöre Sor","speak" to "Yanıtı seslendir",
        "analysis" to "TEKNİK ANALİZ","analysis_desc" to "Kısa teknik video yükle, düzeltme noktalarını incele ve antrenör onaylı referansları kullan.",
        "open_analysis" to "Teknik analizi aç","voice_ready" to "Sesli soru hazır.","voice_unavailable" to "Bu cihazda ses girişi kullanılamıyor."
    )
    val pack=when(code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

fun rsAiAnswerV162(code:String,question:String):String{
    val kind=when{
        question.contains("kick",true)||question.contains("trap",true)||question.contains("patada",true)||question.contains("coup de pied",true)->"kick"
        question.contains("jab",true)||question.contains("cross",true)->"punch"
        question.contains("guard",true)||question.contains("dekking",true)||question.contains("garde",true)->"guard"
        else->"base"
    }
    return when(code){
        "nl"->when(kind){
            "kick"->"Houd je basis stabiel, draai vanuit de heup, houd de tegenovergestelde hand hoog en kom direct terug in je houding."
            "punch"->"Blijf ontspannen in je schouders, draai vanuit vloer en heup, houd je dekking compact en breng je hand recht terug naar je gezicht."
            "guard"->"Bescherm je kin, houd je ellebogen gecontroleerd en laat je handen na elke techniek direct terugkeren."
            else->"Focus eerst op balans, dekking, gecontroleerde rotatie, afstand en een zuivere terugkeer naar je houding."
        }
        "pt"->when(kind){
            "kick"->"Mantém a base estável, roda pela anca, mantém a mão oposta alta e regressa imediatamente à posição."
            "punch"->"Mantém os ombros relaxados, roda a partir do chão e da anca, guarda compacta e mão de volta ao rosto."
            "guard"->"Protege o queixo, controla os cotovelos e faz as mãos regressarem à guarda após cada técnica."
            else->"Foca primeiro no equilíbrio, guarda, rotação controlada, distância e regresso limpo à posição."
        }
        "es"->when(kind){
            "kick"->"Mantén la base estable, gira desde la cadera, conserva la mano contraria alta y vuelve de inmediato a la guardia."
            "punch"->"Relaja los hombros, gira desde el suelo y la cadera, mantén una guardia compacta y recupera la mano directamente a la cara."
            "guard"->"Protege la barbilla, controla los codos y devuelve las manos a la guardia después de cada técnica."
            else->"Concéntrate primero en equilibrio, guardia, rotación controlada, distancia y una recuperación limpia."
        }
        "fr"->when(kind){
            "kick"->"Garde une base stable, tourne avec la hanche, maintiens la main opposée haute et reviens immédiatement en garde."
            "punch"->"Garde les épaules détendues, tourne depuis le sol et la hanche, conserve une garde compacte et ramène la main au visage."
            "guard"->"Protège le menton, contrôle les coudes et ramène les mains en garde après chaque technique."
            else->"Travaille d’abord l’équilibre, la garde, la rotation contrôlée, la distance et un retour propre en position."
        }
        "de"->when(kind){
            "kick"->"Halte deine Basis stabil, drehe aus der Hüfte, halte die Gegenhand oben und kehre sofort in die Stellung zurück."
            "punch"->"Bleib in den Schultern locker, drehe aus Boden und Hüfte, halte die Deckung kompakt und bringe die Hand direkt zum Gesicht zurück."
            "guard"->"Schütze das Kinn, kontrolliere die Ellbogen und bringe die Hände nach jeder Technik sofort zurück."
            else->"Konzentriere dich zuerst auf Balance, Deckung, kontrollierte Rotation, Distanz und eine saubere Rückkehr in die Stellung."
        }
        "it"->when(kind){
            "kick"->"Mantieni la base stabile, ruota con l’anca, tieni alta la mano opposta e torna subito in guardia."
            "punch"->"Rilassa le spalle, ruota da terra e dall’anca, mantieni la guardia compatta e riporta la mano al viso."
            "guard"->"Proteggi il mento, controlla i gomiti e riporta le mani in guardia dopo ogni tecnica."
            else->"Concentrati prima su equilibrio, guardia, rotazione controllata, distanza e recupero pulito della posizione."
        }
        "pl"->when(kind){
            "kick"->"Utrzymuj stabilną bazę, obracaj biodro, trzymaj przeciwną rękę wysoko i od razu wracaj do pozycji."
            "punch"->"Rozluźnij barki, obracaj się od podłoża i biodra, trzymaj zwartą gardę i prowadź rękę prosto z powrotem do twarzy."
            "guard"->"Chroń brodę, kontroluj łokcie i po każdej technice natychmiast wracaj rękami do gardy."
            else->"Najpierw skup się na równowadze, gardzie, kontrolowanej rotacji, dystansie i czystym powrocie do pozycji."
        }
        "tr"->when(kind){
            "kick"->"Duruşunu dengede tut, kalçadan dön, karşı eli yukarıda tut ve vuruştan sonra hemen gardına dön."
            "punch"->"Omuzlarını rahat bırak, yerden ve kalçadan dön, gardını kompakt tut ve elini doğrudan yüzüne geri getir."
            "guard"->"Çeneni koru, dirseklerini kontrol et ve her teknikten sonra ellerini hemen garda geri getir."
            else->"Önce denge, gard, kontrollü dönüş, mesafe ve duruşa temiz geri dönüşe odaklan."
        }
        else->when(kind){
            "kick"->"Keep your base stable, rotate through the hip, keep the opposite hand high, and return immediately to stance after the kick."
            "punch"->"Stay relaxed through the shoulders, rotate from the floor and hip, keep your guard compact, and recover the hand straight back to your face."
            "guard"->"Keep the chin protected, elbows controlled, hands returning to position after every strike."
            else->"Focus first on balance, guard, controlled rotation, distance and a clean recovery to stance."
        }
    }
}
