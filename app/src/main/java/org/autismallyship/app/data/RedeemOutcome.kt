package org.autismallyship.app.data

// What Repository.redeemTicket hands back. Offline is its own case rather than folded into Error,
// because the scanner queues it and retries later instead of just reporting a failure.
sealed class RedeemOutcome {
    data class Redeemed(val ticket: Ticket) : RedeemOutcome()
    data class AlreadyRedeemed(val ticket: Ticket) : RedeemOutcome()
    object NotFound : RedeemOutcome()
    object Offline : RedeemOutcome()
    data class Error(val exception: Exception) : RedeemOutcome()
}
