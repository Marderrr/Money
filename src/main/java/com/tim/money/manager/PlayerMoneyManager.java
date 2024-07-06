package com.tim.money.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoneyManager implements Listener {

    private final Map<UUID, Double> playerMoneyMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private JavaPlugin plugin;

    public PlayerMoneyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initDataFile();
    }

    private void initDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), "playerMoney.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadPlayerMoney() {
        if (dataFile.exists()) {
            dataConfig.getKeys(false).forEach(key -> playerMoneyMap.put(UUID.fromString(key), dataConfig.getDouble(key)));
        }
    }

    public void savePlayerMoney(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerMoneyMap.containsKey(uuid)) {
            dataConfig.set(uuid.toString(), playerMoneyMap.get(uuid));
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAllPlayerMoney() {
        playerMoneyMap.forEach((uuid, money) -> dataConfig.set(uuid.toString(), money));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getPlayerMoney(Player player) {
        return playerMoneyMap.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void setPlayerMoney(Player player, double amount) {
        playerMoneyMap.put(player.getUniqueId(), amount);
        savePlayerMoney(player);
    }

    public void addPlayerMoney(Player player, double amount) {
        playerMoneyMap.merge(player.getUniqueId(), amount, Double::sum);
        savePlayerMoney(player);
    }

    public void removePlayerMoney(Player player, double amount) {
        playerMoneyMap.computeIfPresent(player.getUniqueId(), (uuid, money) -> money - amount);
        savePlayerMoney(player);
    }

    public boolean hasEnoughPlayerMoney(Player player, double amount) {
        return getPlayerMoney(player) >= amount;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!player.hasPlayedBefore() && !playerMoneyMap.containsKey(uuid)) {
            playerMoneyMap.put(uuid, 100.0);
            savePlayerMoney(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (playerMoneyMap.containsKey(uuid)) {
            dataConfig.set(uuid.toString(), playerMoneyMap.get(uuid));
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}