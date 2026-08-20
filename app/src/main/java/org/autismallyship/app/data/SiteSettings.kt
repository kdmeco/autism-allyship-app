package org.autismallyship.app.data

// One document, site_settings/main, rather than a collection, so there is no @DocumentId here. The
// ID is always "main" and a field holding that same string on every read would tell a reader
// nothing.
//
// Every field can be empty and several will stay empty for a while. An empty value means the app
// leaves that line out rather than rendering a blank one, and it never means show a placeholder.
// The five banking fields are the clearest case: until the foundation supplies real ones, the EFT
// section says it is not available yet rather than showing empty rows.
data class SiteSettings(
    val contactEmail: String = "",
    val contactPhone: String = "",
    val whatsappCommunityLink: String = "",
    val whatsappDirectNumber: String = "",
    val instagramUrl: String = "",
    val npoNumber: String = "",
    val pboNumber: String = "",
    val bankName: String = "",
    val bankAccountHolder: String = "",
    val bankAccountNumber: String = "",
    val bankBranchCode: String = "",
    val bankAccountType: String = "",
    val informationOfficerName: String = "",
    val informationOfficerEmail: String = ""
)
