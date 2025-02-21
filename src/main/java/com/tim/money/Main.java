package com.tim.money;

import com.tim.money.manager.PlayerMoneyManager;
import com.tim.money.listener.ShopListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private PlayerMoneyManager playerMoneyManager;

    @Override
    public void onEnable() {
        this.playerMoneyManager = new PlayerMoneyManager(this);
        playerMoneyManager.loadPlayerMoney();

        this.getCommand("money").setExecutor(new MoneyCommand(playerMoneyManager));

        getServer().getPluginManager().registerEvents(new ShopListener(this, playerMoneyManager), this);

        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            boolean result = pluginFolder.mkdir();
            if (result) {
                getLogger().info("Plugin directory created successfully.");
            } else {
                getLogger().severe("Failed to create plugin directory.");
            }
        }
    }

    @Override
    public void onDisable() {
        playerMoneyManager.saveAllPlayerMoney();
    }
    
}
