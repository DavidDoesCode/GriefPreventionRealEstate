package me.EtienneDx.RealEstate;

/**
 * Player-facing transaction events that were previously sent via Essentials mail.
 * Display text is rebuilt from {@link Messages} templates.
 */
public enum TransactionEventType {
    CLAIM_SOLD,
    RENT_START_OWNER,
    RENT_PAYMENT_BUYER,
    RENT_PAYMENT_OWNER,
    RENT_CANCEL_BUYER,
    LEASE_START_OWNER,
    LEASE_PAYMENT_BUYER,
    LEASE_PAYMENT_OWNER,
    LEASE_PAYMENT_BUYER_FINAL,
    LEASE_PAYMENT_OWNER_FINAL,
    LEASE_CANCEL_BUYER,
    LEASE_CANCEL_OWNER,
    EXIT_CREATED,
    EXIT_ACCEPTED,
    EXIT_REJECTED,
    EXIT_CANCELLED;

    /**
     * Rebuilds the chat message for this event using the current language templates.
     *
     * @param messages message pack
     * @param record stored fields
     * @return formatted message including the chat prefix
     */
    public String format(Messages messages, TransactionRecord record) {
        String other = nz(record.otherName);
        String claim = nz(record.claimType);
        String loc = nz(record.location);
        String amount = nz(record.amount);
        String extra = nz(record.extra);
        switch (this) {
            case CLAIM_SOLD:
                return Messages.getMessage(messages.msgInfoClaimOwnerSold, other, claim, amount, loc);
            case RENT_START_OWNER:
                return Messages.getMessage(messages.msgInfoClaimOwnerRented, other, claim, amount, loc);
            case RENT_PAYMENT_BUYER:
                return Messages.getMessage(messages.msgInfoClaimInfoRentPaymentBuyer, claim, loc, amount);
            case RENT_PAYMENT_OWNER:
                return Messages.getMessage(messages.msgInfoClaimInfoRentPaymentOwner, other, claim, loc, amount);
            case RENT_CANCEL_BUYER:
                return Messages.getMessage(messages.msgInfoClaimInfoRentPaymentBuyerCancelled, claim, loc, amount);
            case LEASE_START_OWNER:
                return Messages.getMessage(messages.msgInfoClaimOwnerLeaseStarted, other, claim, amount, loc, extra);
            case LEASE_PAYMENT_BUYER:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentBuyer, claim, loc, amount, extra);
            case LEASE_PAYMENT_OWNER:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentOwner, other, claim, loc, amount, extra);
            case LEASE_PAYMENT_BUYER_FINAL:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentBuyerFinal, claim, loc, amount);
            case LEASE_PAYMENT_OWNER_FINAL:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentOwnerFinal, other, claim, loc, amount);
            case LEASE_CANCEL_BUYER:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentBuyerCancelled, claim, loc, amount);
            case LEASE_CANCEL_OWNER:
                return Messages.getMessage(messages.msgInfoClaimInfoLeasePaymentOwnerCancelled, other, claim, loc, amount);
            case EXIT_CREATED:
                return Messages.getMessage(messages.msgInfoExitOfferCreatedByOther, other, claim, amount, loc);
            case EXIT_ACCEPTED:
                return Messages.getMessage(messages.msgInfoExitOfferAcceptedByOther, other, claim, amount, loc);
            case EXIT_REJECTED:
                return Messages.getMessage(messages.msgInfoExitOfferRejectedByOther, other, claim, loc);
            case EXIT_CANCELLED:
                return Messages.getMessage(messages.msgInfoExitOfferCancelledByOther, other, claim, loc);
            default:
                return Messages.getMessage("$bUnknown transaction.");
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
