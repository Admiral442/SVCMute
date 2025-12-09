package net.envexus.svcmute.util;

import net.envexus.svcmute.SVCMute;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteHelper {
    private static final String URL = "jdbc:sqlite:plugins/SVCMute/mutes.db";

    public SQLiteHelper(SVCMute svcMute) {
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS mutes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL," +
                    "unmute_time LONG NOT NULL)";
            stmt.execute(sql);
            
            // Create history table
            String historySql = "CREATE TABLE IF NOT EXISTS mute_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "muted_by TEXT NOT NULL," +
                    "mute_time LONG NOT NULL," +
                    "unmute_time LONG NOT NULL," +
                    "duration LONG NOT NULL," +
                    "unmuted_by TEXT," +
                    "unmuted_at LONG," +
                    "expired BOOLEAN DEFAULT 0)";
            stmt.execute(historySql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addMute(String uuid, long unmuteTime) {
        String sql = "INSERT INTO mutes(uuid, unmute_time) VALUES(?, ?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setLong(2, unmuteTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void addMuteHistory(String uuid, String playerName, String mutedBy, long muteTime, long unmuteTime, long duration) {
        String sql = "INSERT INTO mute_history(uuid, player_name, muted_by, mute_time, unmute_time, duration) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, playerName);
            pstmt.setString(3, mutedBy);
            pstmt.setLong(4, muteTime);
            pstmt.setLong(5, unmuteTime);
            pstmt.setLong(6, duration);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getUnmuteTime(String uuid) {
        String sql = "SELECT unmute_time FROM mutes WHERE uuid = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("unmute_time");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeMute(String uuid) {
        String sql = "DELETE FROM mutes WHERE uuid = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void markMuteAsManuallyUnmuted(String uuid, String unmutedBy) {
        String sql = "UPDATE mute_history SET unmuted_by = ?, unmuted_at = ?, expired = 0 WHERE uuid = ? AND unmuted_by IS NULL ORDER BY id DESC LIMIT 1";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, unmutedBy);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void markMuteAsExpired(String uuid) {
        String sql = "UPDATE mute_history SET expired = 1 WHERE uuid = ? AND unmuted_by IS NULL ORDER BY id DESC LIMIT 1";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isMuted(String string) {
        String sql = "SELECT * FROM mutes WHERE uuid = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, string);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public ResultSet getMuteHistory(String uuid) {
        String sql = "SELECT * FROM mute_history WHERE uuid = ? ORDER BY mute_time DESC";
        try {
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, uuid);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
