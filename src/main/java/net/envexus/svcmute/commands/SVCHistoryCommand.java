package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@CommandAlias("svchistory")
@CommandPermission("voicechat.history")
@Description("View mute history for a player.")
public class SVCHistoryCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SVCHistoryCommand(SQLiteHelper db) {
        this.db = db;
    }

    @Default
    @Syntax("<player>")
    @Description("View mute history for a player.")
    @CommandCompletion("@players")
    public void onHistory(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        UUID playerUUID;

        if (player != null) {
            playerUUID = player.getUniqueId();
        } else {
            Player offlinePlayer = Bukkit.getOfflinePlayer(playerName).getPlayer();
            if (offlinePlayer == null) {
                sender.sendMessage("§cPlayer not found.");
                return;
            }
            playerUUID = offlinePlayer.getUniqueId();
        }

        ResultSet rs = db.getMuteHistory(playerUUID.toString());
        if (rs == null) {
            sender.sendMessage("§cFailed to retrieve mute history.");
            return;
        }

        try {
            boolean hasHistory = false;
            sender.sendMessage("§6§l========== SVC Mute History for " + playerName + " ==========");

            while (rs.next()) {
                hasHistory = true;
                int id = rs.getInt("id");
                String mutedBy = rs.getString("muted_by");
                long muteTime = rs.getLong("mute_time");
                long unmuteTime = rs.getLong("unmute_time");
                long duration = rs.getLong("duration");
                String unmutedBy = rs.getString("unmuted_by");
                Long unmutedAt = rs.getLong("unmuted_at");
                boolean expired = rs.getBoolean("expired");

                String muteTimeStr = dateFormat.format(new Date(muteTime));
                String durationStr = formatDuration(duration);

                sender.sendMessage("§e#" + id + " §7- §aMuted by: §f" + mutedBy);
                sender.sendMessage("  §7Date: §f" + muteTimeStr);
                sender.sendMessage("  §7Duration: §f" + durationStr);

                if (unmutedBy != null) {
                    String unmutedAtStr = dateFormat.format(new Date(unmutedAt));
                    sender.sendMessage("  §7Status: §aUnmuted by " + unmutedBy + " at " + unmutedAtStr);
                } else if (expired) {
                    sender.sendMessage("  §7Status: §eExpired");
                } else {
                    sender.sendMessage("  §7Status: §cActive");
                }

                sender.sendMessage("");
            }

            if (!hasHistory) {
                sender.sendMessage("§7No mute history found for this player.");
            }

            sender.sendMessage("§6§l================================================");
            rs.close();
        } catch (SQLException e) {
            sender.sendMessage("§cError reading mute history.");
            e.printStackTrace();
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
}
