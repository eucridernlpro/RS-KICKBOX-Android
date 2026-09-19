package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class RsCloudBillingSummaryV77(
    val plan:String,
    val active:Boolean,
    @SerialName("membership_status") val membershipStatus:String,
    @SerialName("amount_cents") val amountCents:Int,
    val currency:String,
    @SerialName("current_period_end") val currentPeriodEnd:String?=null
)

@Serializable
data class RsCloudInvoiceV77(
    val id:String,
    @SerialName("invoice_number") val invoiceNumber:String,
    @SerialName("student_id") val studentId:String?=null,
    @SerialName("student_email") val studentEmail:String?=null,
    @SerialName("student_name") val studentName:String?=null,
    @SerialName("period_label") val periodLabel:String,
    @SerialName("amount_cents") val amountCents:Int,
    val currency:String,
    val status:String,
    @SerialName("due_at") val dueAt:String?=null,
    @SerialName("paid_at") val paidAt:String?=null,
    @SerialName("created_at") val createdAt:String
)

fun rsCloudDateLabelV77(raw:String?):String{
    if(raw.isNullOrBlank())return "—"
    return runCatching{
        Instant.parse(raw).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.getDefault()))
    }.getOrDefault(raw)
}

suspend fun rsMyBillingSummaryV77():Result<RsCloudBillingSummaryV77?> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_billing_summary")
        .decodeList<RsCloudBillingSummaryV77>()
        .firstOrNull()
}

suspend fun rsMyInvoicesV77():Result<List<RsCloudInvoiceV77>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_invoices")
        .decodeList<RsCloudInvoiceV77>()
}

suspend fun rsStaffInvoicesV77():Result<List<RsCloudInvoiceV77>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_invoice_catalog")
        .decodeList<RsCloudInvoiceV77>()
}

suspend fun rsCreateCloudInvoiceV77(
    studentEmail:String,
    periodLabel:String,
    amountCents:Int
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_invoice",
        buildJsonObject{
            put("p_student_email",studentEmail.trim())
            put("p_period_label",periodLabel.trim())
            put("p_amount_cents",amountCents)
        }
    )
    Unit
}

suspend fun rsSetCloudInvoiceStatusV77(
    invoiceId:String,
    status:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_invoice_status",
        buildJsonObject{
            put("p_invoice_id",invoiceId)
            put("p_status",status)
        }
    )
    Unit
}

suspend fun rsDeleteCloudInvoiceV77(invoiceId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_invoice",
        buildJsonObject{put("p_invoice_id",invoiceId)}
    )
    Unit
}
