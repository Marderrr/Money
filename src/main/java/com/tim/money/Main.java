package com.tim.money;

import com.tim.money.manager.PlayerMoneyManager;
import com.tim.money.listener.ShopListener;
import com.tim.money.manager.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {
    private PlayerMoneyManager playerMoneyManager;
    private ShopListener shopListener;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        playerMoneyManager = new PlayerMoneyManager(this);
        shopManager = new ShopManager(this);
        shopListener = new ShopListener(this, playerMoneyManager);

        playerMoneyManager.loadPlayerMoney();
        shopManager.loadShops();

        this.getCommand("money").setExecutor(new MoneyCommand(playerMoneyManager));

        getServer().getPluginManager().registerEvents(shopListener, this);

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
        shopManager.saveShops();
    }

}