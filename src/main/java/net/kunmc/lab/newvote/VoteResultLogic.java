package net.kunmc.lab.newvote;

import org.bukkit.ChatColor;

import java.util.List;

public class VoteResultLogic {

    static void sendVotingDestination(List<String> senders, List<String> receivers, NewVote plugin) {
        plugin.forEachOnlinePlayer(player -> {
            player.sendMessage(ChatColor.GOLD + "投票先を開示します。");
            player.sendMessage(ChatColor.AQUA + "Tabを押してスコアボードを確認してください。");
        });

        int loopCount = Math.min(senders.size(), receivers.size());
        for (int i = 0; i < loopCount; i++) {
            ScoreBoardLogic.setVoteResult(senders.get(i), receivers.get(i), plugin);
        }
    }
}
