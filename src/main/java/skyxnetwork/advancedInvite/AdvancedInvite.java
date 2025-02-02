package skyxnetwork.advancedInvite;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AdvancedInvite extends JavaPlugin implements Listener {
    private final Map<String, String> invitations = new HashMap<>(); // invité -> invitant
    private final Set<String> confirmed = new HashSet<>(); // joueurs déjà confirmés

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("invite").setExecutor(new InviteCommand());
        getCommand("confirm").setExecutor(new ConfirmCommand());
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public class InviteCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return false;
            Player inviter = (Player) sender;

            if (args.length != 1) {
                inviter.sendMessage("§cUsage: /invite <player>");
                return true;
            }

            String inviteeName = args[0];
            invitations.put(inviteeName, inviter.getName());
            inviter.sendMessage("§aInvitation sent to " + inviteeName + "!");
            return true;
        }
    }

    public class ConfirmCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return false;
            Player invitee = (Player) sender;

            if (args.length != 1) {
                invitee.sendMessage("§cUsage: /confirm <player>");
                return true;
            }

            String inviterName = args[0];
            if (!invitations.containsKey(invitee.getName()) || !invitations.get(invitee.getName()).equals(inviterName)) {
                invitee.sendMessage("§cNo invitation found from " + inviterName + ".");
                return true;
            }

            if (confirmed.contains(invitee.getName())) {
                invitee.sendMessage("§cYou have already confirmed an invitation.");
                return true;
            }

            Player inviter = Bukkit.getPlayer(inviterName);
            if (inviter != null && !inviter.getAddress().getAddress().equals(invitee.getAddress().getAddress())) {
                List<String> commands = getConfig().getStringList("On-Validation.command");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", inviterName));
                }
                inviter.sendMessage("§aYour friend " + invitee.getName() + " confirmed the invitation!");
                invitee.sendMessage("§aConfirmation successful!");
                confirmed.add(invitee.getName());
            } else {
                invitee.sendMessage("§cError: Similar IP or guest not found.");
            }
            return true;
        }
    }
}