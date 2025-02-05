package skyxnetwork.advancedInvite;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class AdvancedInvite extends JavaPlugin implements Listener {

    private FileConfiguration statsConfig;
    private File statsFile;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        createConfig();
        loadStats();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AdvancedInvitePlaceholder(this).register();
        }
    }


    @Override
    public void onDisable() {
        saveStats();
    }

    private void createConfig() {
        saveDefaultConfig();
    }

    private void loadStats() {
        statsFile = new File(getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            statsFile.getParentFile().mkdirs();
            saveResource("stats.yml", false);
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    private void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getStatsConfig() {
        return statsConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invite") && sender instanceof Player) {
            handleInvite((Player) sender, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("confirm") && sender instanceof Player) {
            handleConfirm((Player) sender, args);
            return true;
        }
        return false;
    }

    private void handleInvite(Player inviter, String[] args) {
        if (args.length != 1) {
            inviter.sendMessage("§cUsage: /invite <player>");
            return;
        }

        OfflinePlayer invitee = Bukkit.getOfflinePlayer(args[0]);

        // Vérifie si le joueur a déjà rejoint le serveur
        if (!invitee.hasPlayedBefore() && !invitee.isOnline()) {
            inviter.sendMessage("§cThis player has never joined the server or doesn't exist.");
            return;
        }

        List<String> pendingInvites = statsConfig.getStringList("Pending." + inviter.getName());
        if (!pendingInvites.contains(invitee.getName())) {
            pendingInvites.add(invitee.getName());
            statsConfig.set("Pending." + inviter.getName(), pendingInvites);
            inviter.sendMessage("§aYou invited " + invitee.getName() + ".");

            if (invitee.isOnline()) {
                Player onlineInvitee = invitee.getPlayer();
                if (onlineInvitee != null) {
                    onlineInvitee.sendMessage("§e" + inviter.getName() + " invited you. Use /confirm " + inviter.getName() + " to confirm.");
                }
            }

            saveStats();
        } else {
            inviter.sendMessage("§cYou have already invited this player.");
        }
    }

    private void handleConfirm(Player confirmer, String[] args) {
        if (args.length != 1) {
            confirmer.sendMessage("§cUsage: /confirm <inviter>");
            return;
        }
        String inviterName = args[0];
        List<String> pendingInvites = statsConfig.getStringList("Pending." + inviterName);

        if (pendingInvites.contains(confirmer.getName())) {
            String ip = confirmer.getAddress().getAddress().getHostAddress();
            List<String> usedIps = statsConfig.getStringList("UsedIPs." + inviterName);

            if (usedIps.contains(ip)) {
                confirmer.sendMessage("§cYou cannot confirm invitations from the same IP multiple times.");
                return;
            }

            usedIps.add(ip);
            statsConfig.set("UsedIPs." + inviterName, usedIps);

            pendingInvites.remove(confirmer.getName());
            statsConfig.set("Pending." + inviterName, pendingInvites);

            int invites = statsConfig.getInt("Leadboard." + inviterName + ".invites", 0) + 1;
            statsConfig.set("Leadboard." + inviterName + ".invites", invites);

            confirmer.sendMessage("§aYou have confirmed the invitation from " + inviterName + ".");
            Player inviter = Bukkit.getPlayer(inviterName);
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage("§a" + confirmer.getName() + " has confirmed your invitation. You have " + invites + " invites.");
            }

            // 🎁 Donner des récompenses aux deux joueurs
            giveRewards(confirmer, inviter, invites);

            saveStats();
        } else {
            confirmer.sendMessage("§cNo pending invitation from " + inviterName + ".");
        }
    }

    private void giveRewards(Player confirmer, Player inviter, int invites) {
        FileConfiguration config = getConfig();

        // Récompenses pour le confirmeur
        List<String> confirmerCommands = config.getStringList("command.Confirmer");
        for (String command : confirmerCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", confirmer.getName()));
        }

        // Récompenses pour l'inviteur
        if (inviter != null) {
            List<String> inviterCommands = config.getStringList("command.Inviter");
            for (String command : inviterCommands) {
                command = command.replace("%player%", inviter.getName())
                        .replace("%Confirmer%", confirmer.getName())
                        .replace("%invites%", String.valueOf(invites));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        // 🔊 Jouer les sons si activé
        if (config.getBoolean("sounds.enabled")) {
            playSoundSafely(inviter, config.getString("sounds.on-reward"));
            playSoundSafely(confirmer, config.getString("sounds.on-confirm"));
        }
    }

    // ✅ Sécurisation de la lecture des sons
    private void playSoundSafely(Player player, String soundName) {
        if (player != null && soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid sound in config: " + soundName);
            }
        }
    }

    // 🚀 Rappel des invitations en attente lors de la connexion
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // Planifie le rappel 10 secondes après la connexion (200 ticks)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Object inviterName : statsConfig.getConfigurationSection("Pending") != null
                    ? statsConfig.getConfigurationSection("Pending").getKeys(false)
                    : List.of()) {
                List<String> pendingInvites = statsConfig.getStringList("Pending." + inviterName);
                if (pendingInvites.contains(playerName)) {
                    player.sendMessage("§eYou have a pending invitation from " + inviterName + ". Use /confirm " + inviterName + " to confirm.");
                }
            }
        }, 200L); // 200 ticks = 10 secondes
    }
}