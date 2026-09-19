package com.rskickbox.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

data class RsPromoItemV45(
    val id:String,
    val title:String,
    val imageUri:String,
    val externalUrl:String,
    val active:Boolean
)

data class RsBookConfigV45(
    val title:String="VAN STILTE NAAR STRIJD",
    val coverUri:String="",
    val amazonUrl:String="",
    val previewPdfUri:String="",
    val fullPdfUri:String="",
    val accessTier:String="PRO",
    val giftedEmails:Set<String> = emptySet()
)

private fun rsPromoDirV45(context:android.content.Context)=
    File(context.filesDir,"rs_books").apply{mkdirs()}

private fun rsCopyBookPdfV45(context:android.content.Context,source:Uri,label:String):String{
    val dir=rsPromoDirV45(context)
    dir.listFiles()?.filter{it.name.startsWith(label+"_") }?.forEach{it.delete()}
    val out=File(dir,label+"_"+UUID.randomUUID()+".pdf")
    context.contentResolver.openInputStream(source)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString()
}

private fun rsDeleteBookPdfV45(context:android.content.Context,uriString:String){
    if(uriString.isBlank())return
    runCatching{
        val uri=Uri.parse(uriString)
        if(uri.scheme!="file")return
        val file=uri.path?.let(::File)?:return
        val root=rsPromoDirV45(context).canonicalFile
        val target=file.canonicalFile
        if(target.parentFile==root && target.exists())target.delete()
    }
}

private fun rsEncodePromosV45(items:List<RsPromoItemV45>):String{
    val arr=JSONArray()
    items.forEach{p->
        arr.put(JSONObject().apply{
            put("id",p.id);put("title",p.title);put("imageUri",p.imageUri)
            put("externalUrl",p.externalUrl);put("active",p.active)
        })
    }
    return arr.toString()
}

private fun rsDecodePromosV45(raw:String):List<RsPromoItemV45>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsPromoItemV45(
                    o.optString("id"),o.optString("title"),o.optString("imageUri"),
                    o.optString("externalUrl"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadPromosV45(store:RsStore)=rsDecodePromosV45(store.s("promo_items_v45",""))
private fun rsSavePromosV45(store:RsStore,items:List<RsPromoItemV45>)=
    store.ps("promo_items_v45",rsEncodePromosV45(items))

private fun rsEncodeBookV45(book:RsBookConfigV45)=JSONObject().apply{
    put("title",book.title)
    put("coverUri",book.coverUri)
    put("amazonUrl",book.amazonUrl)
    put("previewPdfUri",book.previewPdfUri)
    put("fullPdfUri",book.fullPdfUri)
    put("accessTier",book.accessTier)
    put("giftedEmails",JSONArray(book.giftedEmails.toList()))
}.toString()

private fun rsLoadBookV45(store:RsStore):RsBookConfigV45{
    val raw=store.s("book_config_v45","")
    if(raw.isBlank())return RsBookConfigV45()
    return runCatching{
        val o=JSONObject(raw)
        val gifted=o.optJSONArray("giftedEmails")
        val emails=buildSet{
            if(gifted!=null)for(i in 0 until gifted.length())add(gifted.optString(i).lowercase())
        }
        RsBookConfigV45(
            title=o.optString("title","VAN STILTE NAAR STRIJD"),
            coverUri=o.optString("coverUri"),
            amazonUrl=o.optString("amazonUrl"),
            previewPdfUri=o.optString("previewPdfUri"),
            fullPdfUri=o.optString("fullPdfUri"),
            accessTier=o.optString("accessTier","PRO"),
            giftedEmails=emails
        )
    }.getOrDefault(RsBookConfigV45())
}

private fun rsSaveBookV45(store:RsStore,book:RsBookConfigV45)=store.ps("book_config_v45",rsEncodeBookV45(book))

fun rsValidExternalV45(url:String)=url.startsWith("https://")||url.startsWith("http://")

fun rsOpenExternalV45(context:android.content.Context,url:String){
    if(!rsValidExternalV45(url))return
    runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
}

private fun rsTierRankV45(tier:String)=when(tier.uppercase()){
    "ALL"->0
    "BASIC"->1
    "PRO"->2
    "ELITE"->3
    else->99
}

private fun rsStudentTierV45(store:RsStore):String{
    val email=store.s("session_student_email","alex@rskickbox.nl")
    return rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}?.plan?.uppercase()?:"PRO"
}

private fun rsCanReadFullV45(store:RsStore,book:RsBookConfigV45):Boolean{
    val email=store.s("session_student_email","alex@rskickbox.nl").lowercase()
    if(email in book.giftedEmails)return true
    val required=rsTierRankV45(book.accessTier)
    if(required==99)return false
    return rsTierRankV45(rsStudentTierV45(store))>=required
}

fun rsPromoUiV45(lang:RsLang,key:String):String{
    val en=mapOf(
        "promo_page" to "Promotions","promo_sub" to "Offers, products and featured RS KICKBOX content.",
        "featured" to "FEATURED PROMOTIONS","book" to "FEATURED BOOK","buy" to "Buy on Amazon",
        "preview" to "Read Preview","read" to "Read Full Book","locked" to "Full book access is not included in your current access level.",
        "manager" to "Promotion Manager","manager_sub" to "Upload clickable thumbnails, external links and control the in-app book.",
        "new_thumb" to "NEW PROMOTION THUMBNAIL","title" to "Title","link" to "External link","image" to "Choose thumbnail",
        "save" to "Save thumbnail","active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "book_manager" to "BOOK PROMOTION & READER","book_title" to "Book title","amazon" to "Amazon link",
        "cover" to "Choose book thumbnail / cover","preview_pdf" to "Upload preview PDF","full_pdf" to "Upload full book PDF",
        "access" to "Full book access","gift" to "Free access emails (comma separated)","save_book" to "Save book settings",
        "open_reader" to "Open Reader","search" to "Search book","next_match" to "Next match","no_match" to "No matching page found.",
        "page" to "Page","zoom" to "Zoom","back" to "Back to library","bookmark" to "Bookmark","bookmarked" to "Bookmarked","bookmarks" to "BOOKMARKS","search_action" to "Search","open_error" to "Could not open this PDF.",
        "thumb_ready" to "Thumbnail ready.","cover_ready" to "Book cover ready.","preview_ready" to "Preview PDF imported.",
        "preview_error" to "Could not import preview PDF.","full_ready" to "Full book PDF imported.",
        "full_error" to "Could not import full book PDF.","book_saved" to "Book settings saved."
    )
    val nl=en+mapOf(
        "promo_page" to "Promoties","promo_sub" to "Aanbiedingen, producten en uitgelichte RS KICKBOX-content.",
        "featured" to "UITGELICHTE PROMOTIES","book" to "UITGELICHT BOEK","buy" to "Kopen op Amazon",
        "preview" to "Preview lezen","read" to "Volledig boek lezen","locked" to "Volledige boektoegang zit niet in je huidige toegangsniveau.",
        "manager" to "Promotiebeheer","manager_sub" to "Upload klikbare thumbnails, externe links en beheer het boek in de app.",
        "new_thumb" to "NIEUWE PROMOTIETHUMBNAIL","title" to "Titel","link" to "Externe link","image" to "Thumbnail kiezen",
        "save" to "Thumbnail opslaan","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen",
        "book_manager" to "BOEKPROMOTIE & READER","book_title" to "Boektitel","amazon" to "Amazon-link",
        "cover" to "Boekthumbnail / cover kiezen","preview_pdf" to "Preview-PDF uploaden","full_pdf" to "Volledig boek-PDF uploaden",
        "access" to "Toegang volledig boek","gift" to "Gratis toegang e-mails (komma gescheiden)","save_book" to "Boekinstellingen opslaan",
        "open_reader" to "Reader openen","search" to "Zoek in boek","next_match" to "Volgende match","no_match" to "Geen overeenkomende pagina gevonden.",
        "page" to "Pagina","back" to "Terug naar bibliotheek","bookmark" to "Bladwijzer","bookmarked" to "Opgeslagen","bookmarks" to "BLADWIJZERS","search_action" to "Zoeken","open_error" to "Deze PDF kon niet worden geopend.","thumb_ready" to "Thumbnail klaar.","cover_ready" to "Boekcover klaar.",
        "preview_ready" to "Preview-PDF geïmporteerd.","preview_error" to "Preview-PDF kon niet worden geïmporteerd.",
        "full_ready" to "Volledig boek-PDF geïmporteerd.","full_error" to "Volledig boek-PDF kon niet worden geïmporteerd.","book_saved" to "Boekinstellingen opgeslagen."
    )
    val pt=en+mapOf(
        "promo_page" to "Promoções","promo_sub" to "Ofertas, produtos e conteúdo RS KICKBOX em destaque.",
        "featured" to "PROMOÇÕES EM DESTAQUE","book" to "LIVRO EM DESTAQUE","buy" to "Comprar na Amazon",
        "preview" to "Ler Prévia","read" to "Ler Livro Completo","locked" to "O livro completo não está incluído no teu nível de acesso atual.",
        "manager" to "Gestor de Promoções","manager_sub" to "Carrega miniaturas clicáveis, links externos e gere o livro dentro da app.",
        "new_thumb" to "NOVA MINIATURA PROMOCIONAL","title" to "Título","link" to "Link externo","image" to "Escolher miniatura",
        "save" to "Guardar miniatura","active" to "ATIVO","inactive" to "INATIVO","delete" to "Eliminar","confirm" to "Confirmar",
        "book_manager" to "PROMOÇÃO DO LIVRO & LEITOR","book_title" to "Título do livro","amazon" to "Link Amazon",
        "cover" to "Escolher miniatura / capa","preview_pdf" to "Carregar PDF de prévia","full_pdf" to "Carregar PDF completo",
        "access" to "Acesso ao livro completo","gift" to "Emails com acesso grátis (separados por vírgula)","save_book" to "Guardar definições do livro",
        "open_reader" to "Abrir Leitor","search" to "Pesquisar no livro","next_match" to "Próximo resultado","no_match" to "Nenhuma página encontrada.",
        "page" to "Página","back" to "Voltar à biblioteca","bookmark" to "Marcador","bookmarked" to "Guardado","bookmarks" to "MARCADORES","search_action" to "Pesquisar","open_error" to "Não foi possível abrir este PDF.","thumb_ready" to "Miniatura pronta.","cover_ready" to "Capa pronta.",
        "preview_ready" to "PDF de prévia importado.","preview_error" to "Não foi possível importar o PDF de prévia.",
        "full_ready" to "PDF completo importado.","full_error" to "Não foi possível importar o PDF completo.","book_saved" to "Definições do livro guardadas."
    )
    val es=en+mapOf(
        "promo_page" to "Promociones","promo_sub" to "Ofertas, productos y contenido destacado de RS KICKBOX.",
        "featured" to "PROMOCIONES DESTACADAS","book" to "LIBRO DESTACADO","buy" to "Comprar en Amazon",
        "preview" to "Leer Vista Previa","read" to "Leer Libro Completo","locked" to "El libro completo no está incluido en tu nivel de acceso actual.",
        "manager" to "Gestor de Promociones","manager_sub" to "Sube miniaturas clicables, enlaces externos y controla el libro dentro de la app.",
        "new_thumb" to "NUEVA MINIATURA PROMOCIONAL","title" to "Título","link" to "Enlace externo","image" to "Elegir miniatura",
        "save" to "Guardar miniatura","active" to "ACTIVO","inactive" to "INACTIVO","delete" to "Eliminar","confirm" to "Confirmar",
        "book_manager" to "PROMOCIÓN DEL LIBRO & LECTOR","book_title" to "Título del libro","amazon" to "Enlace de Amazon",
        "cover" to "Elegir miniatura / portada","preview_pdf" to "Subir PDF de vista previa","full_pdf" to "Subir PDF completo",
        "access" to "Acceso al libro completo","gift" to "Emails con acceso gratis (separados por comas)","save_book" to "Guardar ajustes del libro",
        "open_reader" to "Abrir Lector","search" to "Buscar en el libro","next_match" to "Siguiente resultado","no_match" to "No se encontró ninguna página.",
        "page" to "Página","back" to "Volver a la biblioteca","bookmark" to "Marcador","bookmarked" to "Guardado","bookmarks" to "MARCADORES","search_action" to "Buscar","open_error" to "No se pudo abrir este PDF.","thumb_ready" to "Miniatura lista.","cover_ready" to "Portada lista.",
        "preview_ready" to "PDF de vista previa importado.","preview_error" to "No se pudo importar el PDF de vista previa.",
        "full_ready" to "PDF completo importado.","full_error" to "No se pudo importar el PDF completo.","book_saved" to "Ajustes del libro guardados."
    )
    val fr=en+mapOf(
        "promo_page" to "Promotions","promo_sub" to "Offres, produits et contenu RS KICKBOX mis en avant.",
        "featured" to "PROMOTIONS À LA UNE","book" to "LIVRE À LA UNE","buy" to "Acheter sur Amazon",
        "preview" to "Lire l’Aperçu","read" to "Lire le Livre Complet","locked" to "Le livre complet n’est pas inclus dans ton niveau d’accès actuel.",
        "manager" to "Gestion Promotion","manager_sub" to "Ajoute des miniatures cliquables, des liens externes et gère le livre dans l’app.",
        "new_thumb" to "NOUVELLE MINIATURE PROMO","title" to "Titre","link" to "Lien externe","image" to "Choisir la miniature",
        "save" to "Enregistrer la miniature","active" to "ACTIF","inactive" to "INACTIF","delete" to "Supprimer","confirm" to "Confirmer",
        "book_manager" to "PROMOTION LIVRE & LECTEUR","book_title" to "Titre du livre","amazon" to "Lien Amazon",
        "cover" to "Choisir miniature / couverture","preview_pdf" to "Importer PDF aperçu","full_pdf" to "Importer PDF complet",
        "access" to "Accès livre complet","gift" to "Emails avec accès gratuit (séparés par virgules)","save_book" to "Enregistrer les réglages du livre",
        "open_reader" to "Ouvrir le Lecteur","search" to "Rechercher dans le livre","next_match" to "Résultat suivant","no_match" to "Aucune page correspondante.",
        "page" to "Page","back" to "Retour à la bibliothèque","bookmark" to "Signet","bookmarked" to "Enregistré","bookmarks" to "SIGNETS","search_action" to "Rechercher","open_error" to "Impossible d’ouvrir ce PDF.","thumb_ready" to "Miniature prête.","cover_ready" to "Couverture prête.",
        "preview_ready" to "PDF aperçu importé.","preview_error" to "Impossible d’importer le PDF aperçu.",
        "full_ready" to "PDF complet importé.","full_error" to "Impossible d’importer le PDF complet.","book_saved" to "Réglages du livre enregistrés."
    )
    val de=en+mapOf(
        "promo_page" to "Aktionen","promo_sub" to "Angebote, Produkte und hervorgehobene RS KICKBOX-Inhalte.",
        "featured" to "HERVORGEHOBENE AKTIONEN","book" to "HERVORGEHOBENES BUCH","buy" to "Bei Amazon kaufen",
        "preview" to "Vorschau lesen","read" to "Ganzes Buch lesen","locked" to "Der vollständige Buchzugriff ist in deiner aktuellen Stufe nicht enthalten.",
        "manager" to "Promo-Manager","manager_sub" to "Lade klickbare Miniaturen und externe Links hoch und verwalte das In-App-Buch.",
        "new_thumb" to "NEUE PROMO-MINIATUR","title" to "Titel","link" to "Externer Link","image" to "Miniatur auswählen",
        "save" to "Miniatur speichern","active" to "AKTIV","inactive" to "INAKTIV","delete" to "Löschen","confirm" to "Bestätigen",
        "book_manager" to "BUCHPROMOTION & READER","book_title" to "Buchtitel","amazon" to "Amazon-Link",
        "cover" to "Buchminiatur / Cover wählen","preview_pdf" to "Vorschau-PDF hochladen","full_pdf" to "Ganzes Buch-PDF hochladen",
        "access" to "Vollständiger Buchzugriff","gift" to "E-Mails mit Gratiszugriff (kommagetrennt)","save_book" to "Bucheinstellungen speichern",
        "open_reader" to "Reader öffnen","search" to "Buch durchsuchen","next_match" to "Nächster Treffer","no_match" to "Keine passende Seite gefunden.",
        "page" to "Seite","back" to "Zur Bibliothek","bookmark" to "Lesezeichen","bookmarked" to "Gespeichert","bookmarks" to "LESEZEICHEN","search_action" to "Suchen","open_error" to "Dieses PDF konnte nicht geöffnet werden.","thumb_ready" to "Miniatur bereit.","cover_ready" to "Buchcover bereit.",
        "preview_ready" to "Vorschau-PDF importiert.","preview_error" to "Vorschau-PDF konnte nicht importiert werden.",
        "full_ready" to "Vollständiges PDF importiert.","full_error" to "Vollständiges PDF konnte nicht importiert werden.","book_saved" to "Bucheinstellungen gespeichert."
    )
    val it=en+mapOf(
        "promo_page" to "Promozioni","promo_sub" to "Offerte, prodotti e contenuti RS KICKBOX in evidenza.",
        "featured" to "PROMOZIONI IN EVIDENZA","book" to "LIBRO IN EVIDENZA","buy" to "Acquista su Amazon",
        "preview" to "Leggi Anteprima","read" to "Leggi Libro Completo","locked" to "L’accesso al libro completo non è incluso nel tuo livello attuale.",
        "manager" to "Gestione Promozioni","manager_sub" to "Carica miniature cliccabili, link esterni e gestisci il libro nell’app.",
        "new_thumb" to "NUOVA MINIATURA PROMO","title" to "Titolo","link" to "Link esterno","image" to "Scegli miniatura",
        "save" to "Salva miniatura","active" to "ATTIVO","inactive" to "INATTIVO","delete" to "Elimina","confirm" to "Conferma",
        "book_manager" to "PROMO LIBRO & LETTORE","book_title" to "Titolo del libro","amazon" to "Link Amazon",
        "cover" to "Scegli miniatura / copertina","preview_pdf" to "Carica PDF anteprima","full_pdf" to "Carica PDF completo",
        "access" to "Accesso libro completo","gift" to "Email con accesso gratuito (separate da virgole)","save_book" to "Salva impostazioni libro",
        "open_reader" to "Apri Lettore","search" to "Cerca nel libro","next_match" to "Risultato successivo","no_match" to "Nessuna pagina corrispondente.",
        "page" to "Pagina","back" to "Torna alla libreria","bookmark" to "Segnalibro","bookmarked" to "Salvato","bookmarks" to "SEGNALIBRI","search_action" to "Cerca","open_error" to "Impossibile aprire questo PDF.","thumb_ready" to "Miniatura pronta.","cover_ready" to "Copertina pronta.",
        "preview_ready" to "PDF anteprima importato.","preview_error" to "Impossibile importare il PDF anteprima.",
        "full_ready" to "PDF completo importato.","full_error" to "Impossibile importare il PDF completo.","book_saved" to "Impostazioni libro salvate."
    )
    val pl=en+mapOf(
        "promo_page" to "Promocje","promo_sub" to "Oferty, produkty i wyróżnione treści RS KICKBOX.",
        "featured" to "WYRÓŻNIONE PROMOCJE","book" to "WYRÓŻNIONA KSIĄŻKA","buy" to "Kup na Amazon",
        "preview" to "Czytaj Podgląd","read" to "Czytaj Pełną Książkę","locked" to "Pełny dostęp do książki nie jest dostępny w Twoim obecnym poziomie.",
        "manager" to "Menedżer Promocji","manager_sub" to "Dodawaj klikalne miniatury, linki zewnętrzne i zarządzaj książką w aplikacji.",
        "new_thumb" to "NOWA MINIATURA PROMOCYJNA","title" to "Tytuł","link" to "Link zewnętrzny","image" to "Wybierz miniaturę",
        "save" to "Zapisz miniaturę","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","delete" to "Usuń","confirm" to "Potwierdź",
        "book_manager" to "PROMOCJA KSIĄŻKI & CZYTNIK","book_title" to "Tytuł książki","amazon" to "Link Amazon",
        "cover" to "Wybierz miniaturę / okładkę","preview_pdf" to "Wgraj PDF podglądu","full_pdf" to "Wgraj pełny PDF",
        "access" to "Dostęp do pełnej książki","gift" to "E-maile z darmowym dostępem (oddzielone przecinkami)","save_book" to "Zapisz ustawienia książki",
        "open_reader" to "Otwórz Czytnik","search" to "Szukaj w książce","next_match" to "Następny wynik","no_match" to "Nie znaleziono pasującej strony.",
        "page" to "Strona","back" to "Wróć do biblioteki","bookmark" to "Zakładka","bookmarked" to "Zapisano","bookmarks" to "ZAKŁADKI","search_action" to "Szukaj","open_error" to "Nie udało się otworzyć tego PDF.","thumb_ready" to "Miniatura gotowa.","cover_ready" to "Okładka gotowa.",
        "preview_ready" to "PDF podglądu zaimportowany.","preview_error" to "Nie udało się zaimportować PDF podglądu.",
        "full_ready" to "Pełny PDF zaimportowany.","full_error" to "Nie udało się zaimportować pełnego PDF.","book_saved" to "Ustawienia książki zapisane."
    )
    val tr=en+mapOf(
        "promo_page" to "Promosyonlar","promo_sub" to "Teklifler, ürünler ve öne çıkan RS KICKBOX içerikleri.",
        "featured" to "ÖNE ÇIKAN PROMOSYONLAR","book" to "ÖNE ÇIKAN KİTAP","buy" to "Amazon’dan Satın Al",
        "preview" to "Önizlemeyi Oku","read" to "Tam Kitabı Oku","locked" to "Tam kitap erişimi mevcut erişim seviyene dahil değil.",
        "manager" to "Promosyon Yönetimi","manager_sub" to "Tıklanabilir görseller, harici bağlantılar yükle ve uygulama içi kitabı yönet.",
        "new_thumb" to "YENİ PROMOSYON GÖRSELİ","title" to "Başlık","link" to "Harici bağlantı","image" to "Görsel seç",
        "save" to "Görseli kaydet","active" to "AKTİF","inactive" to "PASİF","delete" to "Sil","confirm" to "Onayla",
        "book_manager" to "KİTAP PROMOSYONU & OKUYUCU","book_title" to "Kitap başlığı","amazon" to "Amazon bağlantısı",
        "cover" to "Kitap görseli / kapak seç","preview_pdf" to "Önizleme PDF yükle","full_pdf" to "Tam kitap PDF yükle",
        "access" to "Tam kitap erişimi","gift" to "Ücretsiz erişim e-postaları (virgülle ayır)","save_book" to "Kitap ayarlarını kaydet",
        "open_reader" to "Okuyucuyu Aç","search" to "Kitapta ara","next_match" to "Sonraki sonuç","no_match" to "Eşleşen sayfa bulunamadı.",
        "page" to "Sayfa","back" to "Kütüphaneye dön","bookmark" to "Yer imi","bookmarked" to "Kaydedildi","bookmarks" to "YER İMLERİ","search_action" to "Ara","open_error" to "Bu PDF açılamadı.","thumb_ready" to "Görsel hazır.","cover_ready" to "Kitap kapağı hazır.",
        "preview_ready" to "Önizleme PDF içe aktarıldı.","preview_error" to "Önizleme PDF içe aktarılamadı.",
        "full_ready" to "Tam kitap PDF içe aktarıldı.","full_error" to "Tam kitap PDF içe aktarılamadı.","book_saved" to "Kitap ayarları kaydedildi."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsPromotionManagerV45(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudPromotionManagerV104(c,store,lang);return}
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var promoTitle by remember{mutableStateOf("")}
    var promoLink by remember{mutableStateOf("")}
    var promoImage by remember{mutableStateOf("")}
    var promoStatus by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val promos=remember(revision){rsLoadPromosV45(store)}
    var book by remember(revision){mutableStateOf(rsLoadBookV45(store))}
    var giftedText by remember(book.giftedEmails){mutableStateOf(book.giftedEmails.joinToString(","))}

    val promoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            rsImportVisualMediaV29(
                context,uri,"MEDIUM",
                onStatus={promoStatus=it},
                onComplete={info->
                    if(promoImage.isNotBlank()&&promoImage!=info.uri)rsDeleteOwnedVisualV29(context,promoImage)
                    promoImage=info.uri
                    promoStatus=rsPromoUiV45(lang,"thumb_ready")
                },
                onError={promoStatus=it}
            )
        }
    }
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            rsImportVisualMediaV29(
                context,uri,"MEDIUM",
                onStatus={promoStatus=it},
                onComplete={info->
                    val old=book.coverUri
                    book=book.copy(coverUri=info.uri)
                    if(old.isNotBlank()&&old!=info.uri)rsDeleteOwnedVisualV29(context,old)
                    promoStatus=rsPromoUiV45(lang,"cover_ready")
                },
                onError={promoStatus=it}
            )
        }
    }
    val previewPdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{
            val old=book.previewPdfUri
            val saved=rsCopyBookPdfV45(context,uri,"preview")
            book=book.copy(previewPdfUri=saved)
            if(old.isNotBlank()&&old!=saved)rsDeleteBookPdfV45(context,old)
            promoStatus=rsPromoUiV45(lang,"preview_ready")
        }.onFailure{promoStatus=rsPromoUiV45(lang,"preview_error")}
    }
    val fullPdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{
            val old=book.fullPdfUri
            val saved=rsCopyBookPdfV45(context,uri,"full")
            book=book.copy(fullPdfUri=saved)
            if(old.isNotBlank()&&old!=saved)rsDeleteBookPdfV45(context,old)
            promoStatus=rsPromoUiV45(lang,"full_ready")
        }.onFailure{promoStatus=rsPromoUiV45(lang,"full_error")}
    }

    fun savePromos(items:List<RsPromoItemV45>){rsSavePromosV45(store,items);revision++}

    RsScroll(c,rsPromoUiV45(lang,"manager"),rsPromoUiV45(lang,"manager_sub")){
        RsPanel(c){
            Text(rsPromoUiV45(lang,"new_thumb"),color=c.bright,fontWeight=FontWeight.Black)
            if(promoImage.isNotBlank())RsUriPreviewV21(promoImage,Modifier.fillMaxWidth().height(150.dp),"CENTER")
            OutlinedButton(
                onClick={promoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"image"))}
            OutlinedTextField(promoTitle,{promoTitle=it.take(80)},label={Text(rsPromoUiV45(lang,"title"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(promoLink,{promoLink=it.trim()},label={Text(rsPromoUiV45(lang,"link"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    savePromos(listOf(RsPromoItemV45(UUID.randomUUID().toString(),promoTitle.trim(),promoImage,promoLink.trim(),true))+promos)
                    promoTitle="";promoLink="";promoImage="";promoStatus=""
                },
                enabled=promoTitle.isNotBlank()&&promoImage.isNotBlank()&&rsValidExternalV45(promoLink),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"save"))}
            if(promoStatus.isNotBlank())Text(promoStatus,color=c.muted,fontSize=10.sp)
        }

        promos.forEach{item->
            RsPanel(c){
                RsUriPreviewV21(item.imageUri,Modifier.fillMaxWidth().height(130.dp),"CENTER")
                Text(item.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(item.externalUrl,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(item.active)rsPromoUiV45(lang,"active") else rsPromoUiV45(lang,"inactive"),color=c.muted)
                    Switch(item.active,{on->savePromos(promos.map{if(it.id==item.id)it.copy(active=on) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==item.id){
                            rsDeleteOwnedVisualV29(context,item.imageUri)
                            savePromos(promos.filterNot{it.id==item.id})
                            pendingDelete=null
                        }else pendingDelete=item.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==item.id)rsPromoUiV45(lang,"confirm") else rsPromoUiV45(lang,"delete"))}
            }
        }

        RsPanel(c){
            Text(rsPromoUiV45(lang,"book_manager"),color=c.bright,fontWeight=FontWeight.Black)
            if(book.coverUri.isNotBlank())RsUriPreviewV21(book.coverUri,Modifier.fillMaxWidth().height(190.dp),"CENTER")
            OutlinedButton(
                onClick={coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"cover"))}
            OutlinedTextField(book.title,{book=book.copy(title=it.take(120))},label={Text(rsPromoUiV45(lang,"book_title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(book.amazonUrl,{book=book.copy(amazonUrl=it.trim())},label={Text(rsPromoUiV45(lang,"amazon"))},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={previewPdfPicker.launch(arrayOf("application/pdf"))},modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"preview_pdf")+(if(book.previewPdfUri.isNotBlank())" ✓" else ""))
            }
            OutlinedButton(onClick={fullPdfPicker.launch(arrayOf("application/pdf"))},modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"full_pdf")+(if(book.fullPdfUri.isNotBlank())" ✓" else ""))
            }
            Text(rsPromoUiV45(lang,"access"),color=c.muted,fontSize=10.sp)
            listOf(
                listOf("ALL","BASIC","PRO"),
                listOf("ELITE","PRIVATE")
            ).forEach{rowTiers->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    rowTiers.forEach{tier->
                        FilterChip(
                            selected=book.accessTier==tier,
                            onClick={book=book.copy(accessTier=tier)},
                            label={Text(tier,fontSize=9.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                    if(rowTiers.size==2)Spacer(Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                giftedText,
                {giftedText=it},
                label={Text(rsPromoUiV45(lang,"gift"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2
            )
            Button(
                onClick={
                    val emails=giftedText.split(',',';','\n').map{it.trim().lowercase()}.filter{it.contains("@")}.toSet()
                    book=book.copy(giftedEmails=emails)
                    rsSaveBookV45(store,book)
                    promoStatus=rsPromoUiV45(lang,"book_saved")
                    revision++
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"save_book"))}
        }
    }
}

@Composable
fun RsPromotionPageV45(c:RsPalette,store:RsStore,lang:RsLang,onOpenBook:()->Unit){
    if(RsSupabaseV60.configured){RsCloudPromotionPageV104(c,store,lang,onOpenBook);return}
    val context=LocalContext.current
    val promos=rsLoadPromosV45(store).filter{it.active&&it.imageUri.isNotBlank()}
    val book=rsLoadBookV45(store)
    val listState=rememberLazyListState()
    LaunchedEffect(promos.size){
        if(promos.size>1){
            var index=0
            while(true){
                delay(3200)
                index=(index+1)%promos.size
                listState.animateScrollToItem(index)
            }
        }
    }
    RsScroll(c,rsPromoUiV45(lang,"promo_page"),rsPromoUiV45(lang,"promo_sub")){
        if(promos.isNotEmpty()){
            Text(rsPromoUiV45(lang,"featured"),color=c.bright,fontWeight=FontWeight.Black)
            LazyRow(
                state=listState,
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                contentPadding=PaddingValues(horizontal=2.dp)
            ){
                items(promos,key={it.id}){item->
                    Surface(
                        color=c.panel,
                        shape=MaterialTheme.shapes.large,
                        modifier=Modifier.width(280.dp).clickable{rsOpenExternalV45(context,item.externalUrl)}
                    ){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            RsUriPreviewV21(item.imageUri,Modifier.fillMaxWidth().height(145.dp),"CENTER")
                            Text(item.title,color=c.bright,fontWeight=FontWeight.Bold,maxLines=2)
                            Text(item.externalUrl,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if(book.coverUri.isNotBlank()||book.amazonUrl.isNotBlank()||book.previewPdfUri.isNotBlank()){
            Text(rsPromoUiV45(lang,"book"),color=c.bright,fontWeight=FontWeight.Black)
            RsPanel(c){
                if(book.coverUri.isNotBlank())RsUriPreviewV21(
                    book.coverUri,
                    Modifier.fillMaxWidth().height(230.dp).clickable{rsOpenExternalV45(context,book.amazonUrl)},
                    "CENTER"
                )
                Text(book.title,color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
                if(rsValidExternalV45(book.amazonUrl))Button(
                    onClick={rsOpenExternalV45(context,book.amazonUrl)},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"buy"))}
                if(book.previewPdfUri.isNotBlank())OutlinedButton(onClick=onOpenBook,modifier=Modifier.fillMaxWidth()){
                    Text(rsPromoUiV45(lang,"open_reader"))
                }
            }
        }
    }
}

@Composable
fun RsBookLibraryV45(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudBookLibraryV104(c,store,lang);return}
    val context=LocalContext.current
    val book=rsLoadBookV45(store)
    var readerUri by remember{mutableStateOf("")}
    var readerTitle by remember{mutableStateOf(book.title)}
    val fullAllowed=rsCanReadFullV45(store,book)

    if(readerUri.isNotBlank()){
        RsPdfBookReaderV45(c,store,lang,readerTitle,readerUri){readerUri=""}
        return
    }

    RsScroll(c,rsRouteTitle(lang,"book","Trainer Book"),book.title){
        RsPanel(c){
            if(book.coverUri.isNotBlank())RsUriPreviewV21(
                book.coverUri,
                Modifier.fillMaxWidth().height(270.dp).clickable{rsOpenExternalV45(context,book.amazonUrl)},
                "CENTER"
            )
            Text(book.title,color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)
            if(rsValidExternalV45(book.amazonUrl))Button(
                onClick={rsOpenExternalV45(context,book.amazonUrl)},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"buy"))}
            if(book.previewPdfUri.isNotBlank())OutlinedButton(
                onClick={readerTitle=book.title+" · "+rsPromoUiV45(lang,"preview");readerUri=book.previewPdfUri},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"preview"))}
            if(book.fullPdfUri.isNotBlank()){
                Button(
                    onClick={readerTitle=book.title;readerUri=book.fullPdfUri},
                    enabled=fullAllowed,
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"read"))}
                if(!fullAllowed)Text(rsPromoUiV45(lang,"locked"),color=c.muted,fontSize=10.sp)
            }
        }
    }
}

@Composable
fun RsBookManagerV45(c:RsPalette,store:RsStore,lang:RsLang){
    RsPromotionManagerV45(c,store,lang)
}

@Composable
fun RsPdfBookReaderV45(c:RsPalette,store:RsStore,lang:RsLang,title:String,uriString:String,onBack:()->Unit){
    val context=LocalContext.current
    val file=remember(uriString){Uri.parse(uriString).path?.let(::File)}
    var renderer by remember(uriString){mutableStateOf<PdfRenderer?>(null)}
    var descriptor by remember(uriString){mutableStateOf<ParcelFileDescriptor?>(null)}
    var pageCount by remember(uriString){mutableIntStateOf(0)}
    var search by remember{mutableStateOf("")}
    var searching by remember{mutableStateOf(false)}
    var matches by remember{mutableStateOf<List<Int>>(emptyList())}
    var matchIndex by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    val readerKey=remember(uriString){uriString.hashCode().toString().replace("-","n")}
    val pageKey=remember(readerKey){"book_reader_page_v57_"+readerKey}
    val bookmarkKey=remember(readerKey){"book_reader_marks_v57_"+readerKey}
    var bookmarks by remember(uriString){
        mutableStateOf(
            store.s(bookmarkKey,"").split(',').mapNotNull{it.toIntOrNull()}.filter{it>=0}.toSet()
        )
    }
    var restored by remember(uriString){mutableStateOf(false)}
    val scope=rememberCoroutineScope()

    DisposableEffect(uriString){
        val opened=runCatching{
            val f=file?:error("Missing PDF file")
            val pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY)
            val pdf=PdfRenderer(pfd)
            descriptor=pfd
            renderer=pdf
            pageCount=pdf.pageCount
        }
        if(opened.isFailure)message=rsPromoUiV45(lang,"open_error")
        onDispose{
            runCatching{renderer?.close()}
            runCatching{descriptor?.close()}
            renderer=null
            descriptor=null
        }
    }

    val pagerState=rememberPagerState(pageCount={max(1,pageCount)})

    LaunchedEffect(pageCount,uriString){
        if(pageCount>0 && !restored){
            val saved=store.s(pageKey,"0").toIntOrNull()?.coerceIn(0,pageCount-1)?:0
            pagerState.scrollToPage(saved)
            restored=true
        }
    }

    LaunchedEffect(pagerState.currentPage,restored,pageCount){
        if(restored && pageCount>0){
            store.ps(pageKey,pagerState.currentPage.coerceIn(0,pageCount-1).toString())
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick=onBack,modifier=Modifier.weight(.35f)){Text(rsPromoUiV45(lang,"back"),fontSize=10.sp)}
            Text(title,color=c.bright,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(.65f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedTextField(
                search,{search=it.take(80)},
                label={Text(rsPromoUiV45(lang,"search"))},
                modifier=Modifier.weight(1f),
                singleLine=true
            )
            Button(
                onClick={
                    if(search.isBlank()||file==null)return@Button
                    searching=true
                    message=""
                    scope.launch{
                        val result=withContext(Dispatchers.IO){
                            runCatching{
                                PDFBoxResourceLoader.init(context)
                                PDDocument.load(file).use{doc->
                                    val stripper=PDFTextStripper()
                                    buildList{
                                        for(p in 1..doc.numberOfPages){
                                            stripper.startPage=p
                                            stripper.endPage=p
                                            val text=stripper.getText(doc)
                                            if(text.contains(search,ignoreCase=true))add(p-1)
                                        }
                                    }
                                }
                            }.getOrDefault(emptyList())
                        }
                        matches=result
                        matchIndex=0
                        searching=false
                        if(result.isNotEmpty())pagerState.animateScrollToPage(result.first())
                        else message=rsPromoUiV45(lang,"no_match")
                    }
                },
                enabled=!searching&&search.isNotBlank(),
                modifier=Modifier.widthIn(min=84.dp)
            ){Text(if(searching)"…" else rsPromoUiV45(lang,"search_action"))}
        }
        if(matches.isNotEmpty())Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text((matchIndex+1).toString()+" / "+matches.size,color=c.muted,fontSize=10.sp)
            TextButton(onClick={
                if(matches.isNotEmpty()){
                    matchIndex=(matchIndex+1)%matches.size
                    scope.launch{pagerState.animateScrollToPage(matches[matchIndex])}
                }
            }){Text(rsPromoUiV45(lang,"next_match"))}
        }
        if(message.isNotBlank())Text(message,color=c.muted,fontSize=10.sp)
        if(pageCount>0){
            HorizontalPager(
                state=pagerState,
                modifier=Modifier.fillMaxWidth().weight(1f)
            ){page->
                val pageOffset=((pagerState.currentPage-page)+pagerState.currentPageOffsetFraction).coerceIn(-1f,1f)
                RsPdfPageV45(
                    c=c,
                    renderer=renderer,
                    page=page,
                    modifier=Modifier.fillMaxSize().graphicsLayer{
                        rotationY=-pageOffset*18f
                        scaleX=1f-(pageOffset.absoluteValue*.04f)
                        scaleY=1f-(pageOffset.absoluteValue*.04f)
                        alpha=1f-(pageOffset.absoluteValue*.18f)
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Text(
                    rsPromoUiV45(lang,"page")+" "+(pagerState.currentPage+1)+" / "+pageCount,
                    color=c.muted,
                    modifier=Modifier.weight(1f)
                )
                val currentMarked=pagerState.currentPage in bookmarks
                OutlinedButton(
                    onClick={
                        val next=bookmarks.toMutableSet()
                        if(currentMarked)next.remove(pagerState.currentPage) else next.add(pagerState.currentPage)
                        bookmarks=next
                        store.ps(bookmarkKey,next.sorted().joinToString(","))
                    }
                ){
                    Text(
                        if(currentMarked)rsPromoUiV45(lang,"bookmarked") else rsPromoUiV45(lang,"bookmark"),
                        fontSize=9.sp
                    )
                }
            }
            if(bookmarks.isNotEmpty()){
                Text(rsPromoUiV45(lang,"bookmarks"),color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    items(bookmarks.sorted()){mark->
                        AssistChip(
                            onClick={scope.launch{pagerState.animateScrollToPage(mark.coerceIn(0,pageCount-1))}},
                            label={Text((mark+1).toString(),fontSize=9.sp)}
                        )
                    }
                }
            }
        }else Box(Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun RsPdfPageV45(c:RsPalette,renderer:PdfRenderer?,page:Int,modifier:Modifier=Modifier){
    var scale by remember(page){mutableFloatStateOf(1f)}
    var offsetX by remember(page){mutableFloatStateOf(0f)}
    var offsetY by remember(page){mutableFloatStateOf(0f)}
    val bitmap by produceState<Bitmap?>(null,renderer,page){
        value=withContext(Dispatchers.IO){
            val r=renderer?:return@withContext null
            synchronized(r){
                runCatching{
                    r.openPage(page).use{p->
                        val targetW=1000
                        val targetH=max(1,(targetW*(p.height.toFloat()/p.width)).toInt())
                        Bitmap.createBitmap(targetW,targetH,Bitmap.Config.ARGB_8888).also{bmp->
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            p.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }.getOrNull()
            }
        }
    }
    Surface(color=c.panel,shape=MaterialTheme.shapes.medium,modifier=modifier){
        if(bitmap!=null)Image(
            bitmap=bitmap!!.asImageBitmap(),
            contentDescription="Book page "+(page+1),
            modifier=Modifier.fillMaxSize()
                .pointerInput(page){
                    detectTransformGestures{_,pan,zoom,_->
                        scale=(scale*zoom).coerceIn(1f,4f)
                        if(scale>1f){
                            offsetX+=pan.x
                            offsetY+=pan.y
                        }else{
                            offsetX=0f;offsetY=0f
                        }
                    }
                }
                .graphicsLayer{
                    scaleX=scale;scaleY=scale
                    translationX=offsetX;translationY=offsetY
                }
        )
    }
}
