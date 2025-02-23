package com.tim.money.file;

import com.tim.money.shop.Shop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ShopFileHandler {
    private final JavaPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public ShopFileHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        initDataFile();
    }

    private void initDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), "playerShop.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }


    public void loadShops(Set<Shop> shops, Set<Block> shopSigns) {
        if (dataFile.exists()) {
            dataConfig.getKeys(false).forEach(key -> {
                String owner = dataConfig.getString(key + ".owner");
                String materialName = dataConfig.getString(key + ".itemMaterial");
                Material itemMaterial = Material.getMaterial(materialName);
                double buy = dataConfig.getDouble(key + ".buy");
                double sell = dataConfig.getDouble(key + ".sell");
                int quantity = dataConfig.getInt(key + ".quantity");

                int x = dataConfig.getInt(key + ".sign.x");
                int y = dataConfig.getInt(key + ".sign.y");
                int z = dataConfig.getInt(key + ".sign.z");
                String worldName = dataConfig.getString(key + ".sign.world");
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    Location location = new Location(world, x, y, z);
                    Shop shop = new Shop(owner, itemMaterial, buy, sell, quantity, location);
                    shops.add(shop);
                    shopSigns.add(location.getBlock());
                }
            });
        }
    }

    public void saveShops(Set<Shop> shops) {
        shops.forEach(shop -> {
            String key = shop.getOwner() + "_" + shop.getItem();
            dataConfig.set(key + ".owner", shop.getOwner());
            dataConfig.set(key + ".itemMaterial", shop.getItem().name());
            dataConfig.set(key + ".buy", shop.getBuyPrice());
            dataConfig.set(key + ".sell", shop.getSellPrice());
            dataConfig.set(key + ".quantity", shop.getQuantity());

            Location location = shop.getLocation();
            dataConfig.set(key + ".sign.x", location.getBlockX());
            dataConfig.set(key + ".sign.y", location.getBlockY());
            dataConfig.set(key + ".sign.z", location.getBlockZ());
            dataConfig.set(key + ".sign.world", location.getWorld().getName());
        });
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeShop(Shop shop) {
        String key = shop.getOwner() + "_" + shop.getItem();
        dataConfig.set(key, null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}