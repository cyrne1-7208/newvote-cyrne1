package net.kunmc.lab.newvote;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ScoreBoardLogic {

    static void setVoteStatus(int pattern, Player sender, NewVote plugin) {
        switch (pattern) {
            case 0:
                if (sender != null) {
                    plugin.runForPlayer(sender, () -> safeSetPlayerListName(plugin, sender,
                            ChatColor.GRAY + " × " + ChatColor.WHITE + sender.getName()));
                }
                break;
            case 1:
                if (sender != null) {
                    plugin.runForPlayer(sender, () -> safeSetPlayerListName(plugin, sender,
                            ChatColor.GOLD + " ✓ " + ChatColor.WHITE + sender.getName()));
                }
                break;
            default:
                plugin.forEachOnlinePlayer(player -> safeSetPlayerListName(plugin, player, player.getName()));
                break;
        }
    }

    static void setVoteResult(String sender, String receiver, NewVote plugin) {
        Player player = plugin.getOnlinePlayer(sender);
        if (player == null) {
            return;
        }
        final boolean hideReceiver = plugin.isYanaRevealActive()
                && plugin.getYanaPlayerName().equals(receiver) && plugin.isYanaFeatureEnabled();
        plugin.runForPlayer(player, () -> safeSetPlayerListName(plugin, player, player.getName() + " : "
                + (hideReceiver ? ChatColor.MAGIC : ChatColor.AQUA) + (hideReceiver ? "????????" : receiver)));
    }

    private static void safeSetPlayerListName(NewVote plugin, Player player, String text) {
        try {
            player.setPlayerListName(text);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to update player list name for " + player.getName() + ": " + ex.getMessage());
        }
    }
}
