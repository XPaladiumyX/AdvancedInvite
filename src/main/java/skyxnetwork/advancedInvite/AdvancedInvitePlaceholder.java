package skyxnetwork.advancedInvite;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> leadboardData = statsConfig.getConfigurationSection("Leadboard").getValues(false);
        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>();

        for (Map.Entry<String, Object> entry : leadboardData.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) entry.getValue();
                if (data.containsKey("invites")) {
                    leaderboard.add(Map.entry(entry.getKey(), (Integer) data.get("invites")));
                }
            }
        }

        leaderboard.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); // Trier en ordre décroissant
        return leaderboard;
    }
}