package dev.agam.skyblockitems.utils;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

    private final SkyBlockItems plugin;
    private Connection connection;

    public DatabaseManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        connect();
        setupTables();
    }

    private void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            File databaseFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database: " + e.getMessage());
        }
    }

    private void setupTables() {
        if (connection == null) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS item_rarities (" +
                    "item_key VARCHAR(255) PRIMARY KEY, " +
                    "rarity_id VARCHAR(255)" +
                    ");");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    public void saveItemRarity(String itemKey, String rarityId) {
        if (connection == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT OR REPLACE INTO item_rarities(item_key, rarity_id) VALUES(?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, itemKey);
                pstmt.setString(2, rarityId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save item rarity: " + e.getMessage());
            }
        });
    }

    public void removeItemRarity(String itemKey) {
        if (connection == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM item_rarities WHERE item_key = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, itemKey);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove item rarity: " + e.getMessage());
            }
        });
    }

    public Map<String, String> loadAllItemRarities() {
        Map<String, String> map = new HashMap<>();
        if (connection == null) return map;
        String sql = "SELECT item_key, rarity_id FROM item_rarities";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("item_key"), rs.getString("rarity_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load item rarities: " + e.getMessage());
        }
        return map;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + e.getMessage());
        }
    }
}
