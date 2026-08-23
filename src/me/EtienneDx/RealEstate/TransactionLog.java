package me.EtienneDx.RealEstate;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;

/**
 * SQLite log of player-facing transaction events and last-read timestamps.
 */
public class TransactionLog {
    /** Newest rows kept per player. */
    public static final int MAX_PER_PLAYER = 200;

    private static final String DB_FILE = RealEstate.pluginDirPath + "transaction_log.db";

    private Connection connection;

    /**
     * Opens the database, applies WAL, and creates tables if needed.
     */
    public void open() {
        try {
            File dbFile = new File(DB_FILE);
            if (dbFile.getParentFile() != null) {
                dbFile.getParentFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery("PRAGMA journal_mode=WAL;")) {
                    rs.next();
                }
                st.execute("PRAGMA busy_timeout=5000;");
                st.execute("CREATE TABLE IF NOT EXISTS player_last_read ("
                        + "player_uuid TEXT PRIMARY KEY, "
                        + "last_read INTEGER NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS player_transactions ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "player_uuid TEXT NOT NULL, "
                        + "created_at INTEGER NOT NULL, "
                        + "event_type TEXT NOT NULL, "
                        + "other_name TEXT, "
                        + "claim_type TEXT, "
                        + "location TEXT, "
                        + "amount TEXT, "
                        + "extra TEXT)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_player_tx_time "
                        + "ON player_transactions(player_uuid, created_at DESC)");
            }
        } catch (SQLException e) {
            RealEstate.instance.log.severe("Could not open transaction log database.");
            e.printStackTrace();
        }
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            connection = null;
        }
    }

    /**
     * Records an event for a player and trims old rows.
     *
     * @param playerId player this row belongs to
     * @param type event type
     * @param otherName other player name, or null
     * @param claimType claim/subclaim keyword
     * @param location formatted location
     * @param amount formatted amount
     * @param extra extra placeholder, or null
     */
    public synchronized void record(UUID playerId, TransactionEventType type, String otherName,
            String claimType, String location, String amount, String extra) {
        if (playerId == null || type == null || connection == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String uuid = playerId.toString();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_transactions "
                    + "(player_uuid, created_at, event_type, other_name, claim_type, location, amount, extra) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, uuid);
                ps.setLong(2, now);
                ps.setString(3, type.name());
                ps.setString(4, otherName);
                ps.setString(5, claimType);
                ps.setString(6, location);
                ps.setString(7, amount);
                ps.setString(8, extra);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM player_transactions WHERE player_uuid = ? AND id NOT IN ("
                    + "SELECT id FROM ("
                    + "SELECT id FROM player_transactions WHERE player_uuid = ? "
                    + "ORDER BY created_at DESC, id DESC LIMIT ?"
                    + "))")) {
                ps.setString(1, uuid);
                ps.setString(2, uuid);
                ps.setInt(3, MAX_PER_PLAYER);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            RealEstate.instance.log.warning("Could not record transaction event for " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * @param playerId player
     * @return last-read epoch millis, or 0 if never read
     */
    public synchronized long getLastRead(UUID playerId) {
        if (playerId == null || connection == null) {
            return 0L;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_read FROM player_last_read WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    /**
     * Marks all current events as read for the player.
     *
     * @param playerId player
     */
    public synchronized void markRead(UUID playerId) {
        if (playerId == null || connection == null) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_last_read (player_uuid, last_read) VALUES (?, ?) "
                + "ON CONFLICT(player_uuid) DO UPDATE SET last_read = excluded.last_read")) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param playerId player
     * @return number of stored events
     */
    public synchronized int count(UUID playerId) {
        if (playerId == null || connection == null) {
            return 0;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_transactions WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param playerId player
     * @return events newer than lastRead
     */
    public synchronized int unreadCount(UUID playerId) {
        if (playerId == null || connection == null) {
            return 0;
        }
        long lastRead = getLastRead(playerId);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM player_transactions WHERE player_uuid = ? AND created_at > ?")) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, lastRead);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Newest-first page of events.
     *
     * @param playerId player
     * @param offset row offset
     * @param limit page size
     * @return records, newest first
     */
    public synchronized List<TransactionRecord> list(UUID playerId, int offset, int limit) {
        List<TransactionRecord> results = new ArrayList<>();
        if (playerId == null || connection == null) {
            return results;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT created_at, event_type, other_name, claim_type, location, amount, extra "
                + "FROM player_transactions WHERE player_uuid = ? "
                + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?")) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionRecord record = new TransactionRecord();
                    record.playerId = playerId;
                    record.createdAt = rs.getLong("created_at");
                    try {
                        record.type = TransactionEventType.valueOf(rs.getString("event_type"));
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    record.otherName = rs.getString("other_name");
                    record.claimType = rs.getString("claim_type");
                    record.location = rs.getString("location");
                    record.amount = rs.getString("amount");
                    record.extra = rs.getString("extra");
                    results.add(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Re-applies bold after color codes so unread lines stay bold.
     *
     * @param colored already color-translated message
     * @return bold version
     */
    public static String emphasizeUnread(String colored) {
        if (colored == null || colored.isEmpty()) {
            return colored;
        }
        char section = ChatColor.COLOR_CHAR;
        StringBuilder sb = new StringBuilder(colored.length() + 16);
        sb.append(section).append('l');
        for (int i = 0; i < colored.length(); i++) {
            char ch = colored.charAt(i);
            sb.append(ch);
            if (ch == section && i + 1 < colored.length()) {
                char code = colored.charAt(++i);
                sb.append(code);
                char lower = Character.toLowerCase(code);
                if ((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f') || lower == 'r') {
                    sb.append(section).append('l');
                }
            }
        }
        return sb.toString();
    }
}
