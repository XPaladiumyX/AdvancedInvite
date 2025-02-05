package skyxnetwork.advancedInvite;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class AdvancedInvitePlaceholder extends PlaceholderExpansion {

    private final AdvancedInvite plugin;

    public AdvancedInvitePlaceholder(AdvancedInvite plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "advancedinvite";
    }

    @Override
    public String getAuthor() {
        return "SkyXNetwork";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        FileConfiguration statsConfig = plugin.getStatsConfig();

        if (identifier.startsWith("leadboard_number_")) {
            int position = Integer.parseInt(identifier.replace("leadboard_number_", ""));
            List<Map.Entry<String, Integer>> leaderboard = getSortedLeaderboard(statsConfig);
            if (position - 1 < leaderboard.size()) {
                return String.valueOf(leaderboard.get(position - 1).getValue());
            }
            return "0";
        }

        if (identifier.startsWith("leadboard_player_")) {
            int position = Integer.parseInt(identifier.replace("leadboard_player_", ""));
            List<Map.Entry<String, Integer>> leaderboard = getSortedLeaderboard(statsConfig);
            if (position - 1 < leaderboard.size()) {
                return leaderboard.get(position - 1).getKey();
            }
            return "N/A";
        }

        return null;
    }

    private List<Map.Entry<String, Integer>> getSortedLeaderboard(FileConfiguration statsConfig) {
        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>();

        if (statsConfig.getConfigurationSection("Leadboard") != null) {
            for (String playerName : statsConfig.getConfigurationSection("Leadboard").getKeys(false)) {
                int invites = statsConfig.getInt("Leadboard." + playerName + ".invites", 0);
                leaderboard.add(new AbstractMap.SimpleEntry<>(playerName, invites));
            }
        }

        leaderboard.sort(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed());
        return leaderboard;
    }
}