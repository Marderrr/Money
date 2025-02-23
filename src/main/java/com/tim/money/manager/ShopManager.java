package com.tim.money.manager;

import com.tim.money.file.ShopFileHandler;
import com.tim.money.shop.Shop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class ShopManager {
    private final Set<Shop> shops = new HashSet<>();
    private final Set<Block> shopSigns = new HashSet<>();
    private final ShopFileHandler shopFileHandler;

    public ShopManager(JavaPlugin plugin) {
        this.shopFileHandler = new ShopFileHandler(plugin);
    }

    public void addShop(Shop shop) {
        shops.add(shop);
        shopSigns.add(shop.getLocation().getBlock());
        saveShops();
    }

    public void removeShop(Shop shop) {
        shops.remove(shop);
        shopSigns.remove(shop.getLocation().getBlock());
        shopFileHandler.removeShop(shop);
        saveShops();
    }

    public Shop getShop(String owner, Material item) {
        return shops.stream()
                .filter(shop -> shop.getOwner().equals(owner) && shop.getItem().equals(item))
                .findFirst()
                .orElse(null);
    }

    public Set<Shop> getShops() {
        return shops;
    }

    public Set<Block> getShopSigns() {
        return shopSigns;
    }

    public void createShop(Block block, String owner, Material itemMaterial, String buySell, int quantity) {
        String[] buySellParts = buySell.split(":");
        double buyPrice = Double.parseDouble(buySellParts[0].substring(1));
        double sellPrice = Double.parseDouble(buySellParts[1].substring(1));
        Location location = block.getLocation();
        Shop shop = new Shop(owner, itemMaterial, buyPrice, sellPrice, quantity, location);
        addShop(shop);
    }

    public void updateShop(Block block, String owner, Material oldMaterial, Material newMaterial, String buySell, int quantity) {
        Shop oldShop = getShop(owner, oldMaterial);
        if (oldShop != null) {
            removeShop(oldShop);
        }
        createShop(block, owner, newMaterial, buySell, quantity);
    }

    public void removeShop(Block block, String owner, Material oldMaterial) {
        Shop oldShop = getShop(owner, oldMaterial);
        if (oldShop != null) {
            removeShop(oldShop);
        }
    }

    public void saveShops() {
        shopFileHandler.saveShops(getShops());
    }

    public void loadShops() {
        shopFileHandler.loadShops(getShops(), getShopSigns());
    }
}