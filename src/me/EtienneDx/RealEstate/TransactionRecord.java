package me.EtienneDx.RealEstate;

import java.util.UUID;

/**
 * A single recorded player-facing transaction event.
 */
public class TransactionRecord {
    /** Player this row belongs to. */
    public UUID playerId;
    /** Epoch millis when the event was recorded. */
    public long createdAt;
    /** Event kind used to rebuild the display message. */
    public TransactionEventType type;
    /** Other player involved, if any. */
    public String otherName;
    /** Claim or subclaim keyword. */
    public String claimType;
    /** Formatted location string. */
    public String location;
    /** Formatted price or amount. */
    public String amount;
    /** Extra placeholder (e.g. remaining lease payments). */
    public String extra;
}
