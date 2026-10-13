package net.kunmc.lab.newvote;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NewVote extends JavaPlugin implements Listener {

    private static final String YANA_PLAYER_NAME = "Yanaaaaa";

    private static boolean vs = false;
    private static boolean vget = false;
    private static boolean vlist = false;

    private static List<String> senderListCurrent = new ArrayList<>();
    private static List<String> receiverListCurrent = new ArrayList<>();
    private static List<String> senderListResult = new ArrayList<>();
    private static List<String> receiverListResult = new ArrayList<>();
    private static List<String> voteTargetList = new ArrayList<>();

    private boolean yanaRevealReserved = false;
    private static boolean yanaRevealActive = false;
    private static boolean yanaFeatureEnabled = true;
    private int yanaCountdownVotes;
    private int rank = 1;
    private String yanaRankLabel;

    private SchedulerLogic scheduler;
    private final Map<UUID, Player> onlinePlayers = new LinkedHashMap<>();
    private final Map<UUID, String> onlinePlayerNames = new LinkedHashMap<>();

    private Path configFile;
    private FileTime configModified;
    private long configSize = -1;
    private boolean voteTargetsDirty = true;

    private boolean debugEnabled;
    private boolean debugLogStackTrace;

    @Override
    public synchronized void onEnable() {
        scheduler = new SchedulerLogic(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.put(player.getUniqueId(), player);
            onlinePlayerNames.put(player.getUniqueId(), player.getName());
        }
        saveDefaultConfig();
        reloadConfigIfChanged();

        boolean allCommandsRegistered = true;
        allCommandsRegistered &= safeRegisterCommand("v", true);
        allCommandsRegistered &= safeRegisterCommand("yvote", false);
        allCommandsRegistered &= safeRegisterCommand("vs", false);
        allCommandsRegistered &= safeRegisterCommand("vget", false);

        if (!allCommandsRegistered) {
            getLogger().severe("Plugin disabled because one or more commands are missing.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("NewVotePlugin by Cyrne1_7208");
    }

    @Override
    public synchronized void onDisable() {
        if (scheduler != null) {
            scheduler.cancelTasks();
        }
        onlinePlayers.clear();
        onlinePlayerNames.clear();
        clearAllRuntimeState();
    }

    @EventHandler
    public synchronized void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        onlinePlayers.put(player.getUniqueId(), player);
        onlinePlayerNames.put(player.getUniqueId(), player.getName());
        voteTargetsDirty = true;
    }

    @EventHandler
    public synchronized void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        onlinePlayers.remove(uuid);
        onlinePlayerNames.remove(uuid);
        voteTargetsDirty = true;
    }

    private void loadDebugConfig() {
        debugEnabled = getConfig().getBoolean("debug.enabled", false);
        debugLogStackTrace = getConfig().getBoolean("debug.logStackTrace", true);
    }

    private void clearAllRuntimeState() {
        vs = false;
        vget = false;
        vlist = false;
        senderListCurrent.clear();
        receiverListCurrent.clear();
        senderListResult.clear();
        receiverListResult.clear();
        voteTargetList.clear();
        voteTargetsDirty = true;
        yanaRevealReserved = false;
        yanaRevealActive = false;
        yanaFeatureEnabled = true;
        yanaCountdownVotes = 0;
        yanaRankLabel = null;
    }

    private boolean safeRegisterCommand(String commandName, boolean tabComplete) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().severe("Command not found in plugin.yml: " + commandName);
            return false;
        }
        command.setExecutor(this);
        if (tabComplete) {
            command.setTabCompleter(this);
        }
        return true;
    }

    @Override
    public synchronized boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            String commandName = cmd.getName().toLowerCase(Locale.ROOT);
            switch (commandName) {
                case "v":
                    return handleVoteCommand(sender, args);
                case "yvote":
                    return handleYvoteCommand(sender, args);
                case "vs":
                    return handleVsCommand(sender, args);
                case "vget":
                    return handleVgetCommand(sender, args);
                default:
                    return false;
            }
        } catch (Exception ex) {
            logError("Unexpected error while handling command", sender, cmd, args, ex);
            sender.sendMessage(ChatColor.RED + "内部エラーが発生しました。管理者にログを共有してください。");
            return true;
        }
    }

    private boolean handleVoteCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /v <投票先>");
            return true;
        }

        if (!vs) {
            sender.sendMessage(ChatColor.RED + "投票は開始されていません。");
            return true;
        }

        Player player = (Player) sender;
        reloadConfigIfChanged();
        loadVoteTargetList();

        String senderUuid = player.getUniqueId().toString();
        if (senderListCurrent.contains(senderUuid)) {
            sender.sendMessage(ChatColor.YELLOW + "あなたはすでに投票済みです。");
            return true;
        }

        String receiver = args[0];
        if (!voteTargetList.contains(receiver)) {
            sender.sendMessage(ChatColor.RED + receiver + " は投票先リストに存在しません。");
            return true;
        }

        senderListCurrent.add(senderUuid);
        receiverListCurrent.add(receiver);
        ScoreBoardLogic.setVoteStatus(1, player, this);
        sender.sendMessage(ChatColor.GREEN + receiver + " に投票しました。");
        debug(() -> "Vote accepted: voter=" + player.getName() + ", target=" + receiver);

        return true;
    }

    private void reloadConfigIfChanged() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml").toPath();
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(configFile, BasicFileAttributes.class);
            if (attributes.lastModifiedTime().equals(configModified) && attributes.size() == configSize) {
                return;
            }
            reloadConfig();
            configModified = attributes.lastModifiedTime();
            configSize = attributes.size();
        } catch (IOException ex) {
            reloadConfig();
        }
        loadDebugConfig();
        voteTargetsDirty = true;
    }

    private void loadVoteTargetList() {
        if (!voteTargetsDirty) {
            return;
        }
        List<String> configuredTargets = getConfig().getStringList("List");
        voteTargetList = new ArrayList<>(new LinkedHashSet<>(configuredTargets.isEmpty()
                ? onlinePlayerNames.values() : configuredTargets));
        voteTargetsDirty = false;
    }

    private boolean handleYvoteCommand(CommandSender sender, String[] args) {
        if (!sender.getName().equals(YANA_PLAYER_NAME)) {
            sender.sendMessage(ChatColor.RED + "このコマンドは使用できません。");
            return true;
        }

        if (args.length == 0) {
            yanaFeatureEnabled = !yanaFeatureEnabled;
            sender.sendMessage(ChatColor.GOLD + (yanaFeatureEnabled ? "やなーもーどおん" : "やなーもーどおふ"));
            debug(() -> "Yana mode toggled: " + yanaFeatureEnabled);
            return true;
        }

        if (args.length == 1) {
            try {
                int parsedRank = Integer.parseInt(args[0]);
                if (parsedRank < 1) {
                    sender.sendMessage(ChatColor.RED + "順位は1以上を指定してください。");
                    return true;
                }
                rank = parsedRank;
                sender.sendMessage(ChatColor.GOLD + "基準を " + parsedRank + " 位以上にしました。");
                debug(() -> "Yana rank threshold updated: " + rank);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "数値で指定してください。");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "コマンド形式: /yvote [順位]");
        return true;
    }

    private boolean handleVsCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /vs");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。必要権限: OP");
            return true;
        }

        if (vlist) {
            ScoreBoardLogic.setVoteStatus(2, asPlayerOrNull(sender), this);
            vlist = false;
            sender.sendMessage(ChatColor.GREEN + "Tab リストから投票先表示を削除しました。");
            debug(() -> "Vote list view cleared.");
            return true;
        }

        if (!vs) {
            scheduler.cancelTasks();
            reloadConfigIfChanged();

            senderListCurrent.clear();
            receiverListCurrent.clear();
            senderListResult.clear();
            receiverListResult.clear();
            voteTargetList.clear();
            voteTargetsDirty = true;

            yanaRevealActive = false;
            yanaRevealReserved = false;

            forEachOnlinePlayer(player -> {
                player.sendMessage(ChatColor.GOLD + "投票が開始されました。");
                player.sendMessage(ChatColor.GREEN + "/v <投票先> で投票してください。");
                ScoreBoardLogic.setVoteStatus(0, player, this);
            });

            vs = true;
            vget = false;
            debug(() -> "Voting started.");
            return true;
        }

        senderListResult = new ArrayList<>(senderListCurrent);
        receiverListResult = new ArrayList<>(receiverListCurrent);
        senderListCurrent.clear();
        receiverListCurrent.clear();

        sendVotingResult(receiverListResult);
        ScoreBoardLogic.setVoteStatus(2, asPlayerOrNull(sender), this);
        voteTargetList.clear();
        voteTargetsDirty = true;
        debug(() -> "Voting closed. resultsCount=" + receiverListResult.size());

        return true;
    }

    private Player asPlayerOrNull(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    private boolean handleVgetCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /vget");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。必要権限: OP");
            return true;
        }

        if (!vget) {
            sender.sendMessage(ChatColor.RED + "投票結果がまだ確定していません。");
            return true;
        }

        VoteResultLogic.sendVotingDestination(senderListResult, receiverListResult, this);
        vs = false;
        vget = false;
        vlist = true;
        debug(() -> "Vote destinations revealed.");

        return true;
    }

    @Override
    public synchronized List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("v") || args.length != 1) {
            return new ArrayList<>();
        }

        reloadConfigIfChanged();
        loadVoteTargetList();

        final String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String name : voteTargetList) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }

    public synchronized void sendVotingResult(List<String> receivers) {
        try {
            Map<String, Integer> voteCountMap = new HashMap<>();
            for (String receiver : receivers) {
                voteCountMap.merge(receiver, 1, Integer::sum);
            }

            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(voteCountMap.entrySet());
            sortedEntries.sort((left, right) -> {
                int byVote = right.getValue().compareTo(left.getValue());
                return byVote != 0 ? byVote : left.getKey().compareTo(right.getKey());
            });

            List<String> resultLines = getVotingResultMessages(sortedEntries, false);
            forEachOnlinePlayer(player -> {
                player.sendMessage(ChatColor.GOLD + "投票結果を表示します。");
                player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
                resultLines.forEach(player::sendMessage);
                player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
            });

            if (yanaRevealReserved && yanaFeatureEnabled) {
                yanaRevealReserved = false;
                yanaRevealActive = true;

                forEachOnlinePlayer(player -> {
                    player.sendTitle(ChatColor.AQUA + "おや？投票結果の様子が...!?", null, 5, 60, 5);
                    playSoundCompat(player, () -> Sound.ENTITY_GENERIC_EXPLODE, () -> Sound.BLOCK_ANVIL_PLACE, 5f, 1f);
                });

                scheduler.runTaskLater(task -> {
                    forEachOnlinePlayer(player -> {
                        player.sendTitle(ChatColor.GREEN + "やなぱわ～", null, 5, 40, 5);
                        playSoundCompat(player, () -> Sound.ENTITY_PLAYER_LEVELUP, () -> Sound.UI_TOAST_CHALLENGE_COMPLETE, 5f, 1f);
                    });
                    vs = false;
                    vget = true;
                    debug(() -> "Yana reveal first stage completed.");
                }, 120L);

                scheduler.runTaskTimer(task -> {
                    if (yanaCountdownVotes >= 0) {
                        final String title = ChatColor.GREEN + yanaRankLabel + "位: " + ChatColor.WHITE
                                + YANA_PLAYER_NAME + ChatColor.GOLD + " [" + yanaCountdownVotes + "票]";
                        forEachOnlinePlayer(player -> {
                            player.sendTitle(
                                    title,
                                    null,
                                    0,
                                    20,
                                    0
                            );
                            playSoundCompat(player, () -> Sound.BLOCK_ANVIL_PLACE, () -> Sound.BLOCK_NOTE_BLOCK_PLING, 5f, 1f);
                        });
                        yanaCountdownVotes--;
                        return;
                    }

                    List<String> rerankLines = getVotingResultMessages(sortedEntries, true);
                    forEachOnlinePlayer(player -> {
                        player.sendMessage(ChatColor.GOLD + "投票結果を再表示します。");
                        player.sendTitle(ChatColor.GOLD + "投票結果を再表示します。", null, 5, 20, 5);
                        player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
                        rerankLines.forEach(player::sendMessage);
                        player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
                    });
                    task.cancel();
                    debug(() -> "Yana reveal second stage completed.");
                }, 200L, 15L);
            } else {
                vs = false;
                vget = true;
            }
        } catch (Exception ex) {
            logError("Failed to send voting result", null, null, null, ex);
            forEachOnlinePlayer(player ->
                    player.sendMessage(ChatColor.RED + "投票結果の表示中にエラーが発生しました。")
            );
            vs = false;
            vget = true;
        }
    }

    private List<String> getVotingResultMessages(List<Map.Entry<String, Integer>> entries, boolean excludeYana) {
        List<String> messages = new ArrayList<>(entries.size());
        int ranking = 0;
        int previousVotes = -1;
        for (Map.Entry<String, Integer> entry : entries) {
            if (excludeYana && entry.getKey().equals(YANA_PLAYER_NAME)) {
                continue;
            }
            int currentVotes = entry.getValue();
            if (currentVotes != previousVotes) {
                ranking++;
            }
            messages.add(ChatColor.GREEN + Integer.toString(ranking) + "位: " + ChatColor.WHITE
                    + entry.getKey() + ChatColor.GOLD + " [" + currentVotes + "票]");
            if (!excludeYana && entry.getKey().equals(YANA_PLAYER_NAME) && ranking <= rank) {
                yanaRevealReserved = true;
                yanaCountdownVotes = currentVotes;
                yanaRankLabel = Integer.toString(ranking);
            }
            previousVotes = currentVotes;
        }
        return messages;
    }

    private void playSoundCompat(Player player, Supplier<Sound> primary, Supplier<Sound> fallback, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), primary.get(), volume, pitch);
        } catch (IllegalArgumentException | NoSuchFieldError ex) {
            try {
                player.playSound(player.getLocation(), fallback.get(), volume, pitch);
            } catch (IllegalArgumentException | NoSuchFieldError fallbackError) {
                getLogger().warning("Unable to play voting sound: " + fallbackError.getMessage());
            }
        }
    }

    void runForPlayer(Player player, Runnable action) {
        scheduler.runForPlayer(player, action);
    }

    synchronized void forEachOnlinePlayer(Consumer<Player> action) {
        for (Player player : new ArrayList<>(onlinePlayers.values())) {
            runForPlayer(player, () -> action.accept(player));
        }
    }

    synchronized Player getOnlinePlayer(String sender) {
        if (sender == null || sender.isEmpty()) {
            return null;
        }
        try {
            return onlinePlayers.get(UUID.fromString(sender));
        } catch (IllegalArgumentException ex) {
            for (Map.Entry<UUID, String> entry : onlinePlayerNames.entrySet()) {
                if (entry.getValue().equals(sender)) {
                    return onlinePlayers.get(entry.getKey());
                }
            }
            return null;
        }
    }

    private void debug(Supplier<String> message) {
        if (!debugEnabled) {
            return;
        }
        getLogger().info("[NewVote][DEBUG] " + message.get());
    }

    private void logError(String context, CommandSender sender, Command cmd, String[] args, Throwable ex) {
        getLogger().severe("[NewVote][ERROR] " + context);
        if (sender != null) {
            getLogger().severe("[NewVote][ERROR] sender=" + asDebugSender(sender));
        }
        if (cmd != null) {
            getLogger().severe("[NewVote][ERROR] command=" + cmd.getName());
        }
        if (args != null) {
            getLogger().severe("[NewVote][ERROR] args=" + Arrays.toString(args));
        }
        getLogger().severe("[NewVote][ERROR] state: vs=" + vs + ", vget=" + vget + ", vlist=" + vlist
                + ", currentVotes=" + receiverListCurrent.size() + ", resultVotes=" + receiverListResult.size());

        if (debugLogStackTrace) {
            ex.printStackTrace();
        } else {
            getLogger().severe("[NewVote][ERROR] message=" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String asDebugSender(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return player.getName() + "(" + player.getUniqueId() + ")";
        }
        return sender.getName() + "(non-player)";
    }

    public synchronized boolean isVotingOpen() {
        return vs;
    }

    public synchronized boolean isVoteResultPhase() {
        return vget;
    }

    public synchronized boolean isVoteListShown() {
        return vlist;
    }

    public synchronized boolean isYanaRevealActive() {
        return yanaRevealActive;
    }

    public synchronized boolean isYanaFeatureEnabled() {
        return yanaFeatureEnabled;
    }

    public String getYanaPlayerName() {
        return YANA_PLAYER_NAME;
    }
}
