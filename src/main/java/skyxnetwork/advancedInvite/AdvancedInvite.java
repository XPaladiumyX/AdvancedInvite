package skyxnetwork.advancedInvite;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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
        Player invitee = Bukkit.getPlayer(args[0]);
        if (invitee == null || !invitee.isOnline()) {
            inviter.sendMessage("§cPlayer not found or offline.");
            return;
        }

        List<String> pendingInvites = statsConfig.getStringList("Pending." + inviter.getName());
        if (!pendingInvites.contains(invitee.getName())) {
            pendingInvites.add(invitee.getName());
            statsConfig.set("Pending." + inviter.getName(), pendingInvites);
            inviter.sendMessage("§aYou invited " + invitee.getName() + ".");
            invitee.sendMessage("§e" + inviter.getName() + " invited you. Use /confirm " + inviter.getName() + " to confirm.");
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
            saveStats();
        } else {
            confirmer.sendMessage("§cNo pending invitation from " + inviterName + ".");
        }
    }
}