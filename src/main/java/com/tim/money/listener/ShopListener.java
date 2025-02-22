package com.tim.money.listener;

import com.tim.money.manager.PlayerMoneyManager;
import com.tim.money.shop.Shop;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShopListener implements Listener {
    private final Set<Shop> shops = new HashSet<>();
    private final Set<Block> shopSigns = new HashSet<>();
    private final PlayerMoneyManager moneyManager;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final JavaPlugin plugin;

    public ShopListener(JavaPlugin plugin, PlayerMoneyManager playerMoneyManager) {
        this.plugin = plugin;
        this.moneyManager = playerMoneyManager;
        initDataFile();
    }

    private void initDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), "playerShop.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadShops() {
        if (dataFile.exists()) {
            dataConfig.getKeys(false).forEach(key -> {
                String owner = dataConfig.getString(key + ".owner");
                String materialName = dataConfig.getString(key + ".itemMaterial");
                Material itemMaterial = Material.getMaterial(materialName);
                double buy = dataConfig.getDouble(key + ".buy");
                double sell = dataConfig.getDouble(key + ".sell");
                int quantity = dataConfig.getInt(key + ".quantity");

                // Load shop sign locations
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

    public void saveShops() {
        shops.forEach(shop -> {
            String key = shop.getOwner() + "_" + shop.getItem();
            dataConfig.set(key + ".owner", shop.getOwner());
            dataConfig.set(key + ".itemMaterial", shop.getItem().name());
            dataConfig.set(key + ".buy", shop.getBuyPrice());
            dataConfig.set(key + ".sell", shop.getSellPrice());
            dataConfig.set(key + ".quantity", shop.getQuantity());

            // Save shop sign locations
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

    private Block getAttachedBlock(Block signBlock) {
        if (signBlock.getBlockData() instanceof WallSign) {
            return signBlock.getRelative(((WallSign) signBlock.getBlockData()).getFacing().getOppositeFace());
        } else {
            return signBlock.getRelative(BlockFace.DOWN);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (shopSigns.contains(block) && !event.getLine(0).equalsIgnoreCase(player.getName())) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this shop!");
            event.setCancelled(true);
            return;
        }

        Block attachedBlock = getAttachedBlock(block);
        boolean wasShop = shopSigns.contains(block);

        Material oldItemMaterial = null;
        if (wasShop) {
            Sign sign = (Sign) block.getState();
            oldItemMaterial = Material.getMaterial(sign.getLine(1).toUpperCase());
        }

        Material newItemMaterial = Material.getMaterial(event.getLine(1).toUpperCase());
        if (newItemMaterial == null) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
                player.sendMessage(block + "\n" + event.getLine(0) + "\n" + oldItemMaterial);
            }
            return;
        }

        if (!(attachedBlock.getState() instanceof Chest) || ((Chest) attachedBlock.getState()).getInventory().getHolder() instanceof DoubleChest) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(0).equalsIgnoreCase(player.getName())) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(2).matches("B\\d+:S\\d+")) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(event.getLine(3));
        } catch (NumberFormatException e) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!wasShop) {
            shopSigns.add(block);
            createShop(block, event.getLine(0), newItemMaterial, event.getLine(2), quantity);
            player.sendMessage(ChatColor.GREEN + "Shop sign created successfully.");
        } else {
            updateShop(block, event.getLine(0), oldItemMaterial, newItemMaterial, event.getLine(2), quantity);
            player.sendMessage(ChatColor.GREEN + "Shop sign updated successfully.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        String owner = sign.getLine(0);
        Player player = event.getPlayer();
        Player ownerPlayer = event.getPlayer().getServer().getPlayerExact(owner);

        if (player.getName().equalsIgnoreCase(owner)) {
            return; // Besitzer kann seinen eigenen Shop nicht nutzen
        }

        Material itemMaterial = Material.getMaterial(sign.getLine(1).toUpperCase());
        if (itemMaterial == null) {
            return;
        }

        String[] buySellParts = sign.getLine(2).split(":");
        double buyPrice = Double.parseDouble(buySellParts[0].substring(1));
        double sellPrice = Double.parseDouble(buySellParts[1].substring(1));
        int quantity = Integer.parseInt(sign.getLine(3));

        Block attachedBlock = getAttachedBlock(block);
        if (!(attachedBlock.getState() instanceof Chest)) {
            return;
        }

        Chest chest = (Chest) attachedBlock.getState();
        Inventory chestInv = chest.getInventory();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            player.sendMessage(String.valueOf(moneyManager.getPlayerMoney(player)));
            if (!moneyManager.hasEnoughPlayerMoney(player, buyPrice)) {
                player.sendMessage(ChatColor.RED + "Not enough money!");
                return;
            }
            if (!chestInv.containsAtLeast(new ItemStack(itemMaterial), quantity)) {
                player.sendMessage(ChatColor.RED + "Shop is out of stock!");
                return;
            }
            if (!player.getInventory().addItem(new ItemStack(itemMaterial, quantity)).isEmpty()) {
                player.sendMessage(ChatColor.RED + "Not enough inventory space!");
                return;
            }
            chestInv.removeItem(new ItemStack(itemMaterial, quantity));
            moneyManager.removePlayerMoney(player, buyPrice);
            if (ownerPlayer != null) moneyManager.addPlayerMoney(ownerPlayer, buyPrice);
            player.sendMessage(ChatColor.GREEN + "You bought " + quantity + " " + itemMaterial + " for " + buyPrice + "!");

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!player.getInventory().containsAtLeast(new ItemStack(itemMaterial), quantity)) {
                player.sendMessage(ChatColor.RED + "You don't have enough items to sell!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(itemMaterial, quantity));
            chestInv.addItem(new ItemStack(itemMaterial, quantity));
            moneyManager.addPlayerMoney(player, sellPrice);
            if (ownerPlayer != null) moneyManager.removePlayerMoney(ownerPlayer, sellPrice);
            player.sendMessage(ChatColor.GREEN + "You sold " + quantity + " " + itemMaterial + " for " + sellPrice + "!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (shopSigns.contains(block)) {
            Sign sign = (Sign) block.getState();
            String owner = sign.getLine(0);
            Material itemMaterial = Material.getMaterial(sign.getLine(1).toUpperCase());

            if (itemMaterial != null) {
                removeShop(block, owner, itemMaterial);
                shopSigns.remove(block);
                event.getPlayer().sendMessage(ChatColor.RED + "Shop sign destroyed and shop entry removed.");
            }
        }
    }

    private void createShop(Block block, String owner, Material itemMaterial, String buySell, int quantity) {
        String[] buySellParts = buySell.split(":");
        double buyPrice = Double.parseDouble(buySellParts[0].substring(1));
        double sellPrice = Double.parseDouble(buySellParts[1].substring(1));
        Location location = block.getLocation();
        Shop shop = new Shop(owner, itemMaterial, buyPrice, sellPrice, quantity, location);
        shops.add(shop);
        saveShops();
    }

    private void updateShop(Block block, String owner, Material oldMaterial, Material newMaterial, String buySell, int quantity) {
        Shop oldShop = shops.stream()
                .filter(shop -> shop.getOwner().equals(owner) && shop.getItem().equals(oldMaterial))
                .findFirst()
                .orElse(null);

        if (oldShop != null) {
            // Remove the old shop entry from the configuration file
            String oldKey = owner + "_" + oldShop.getItem().name();
            dataConfig.set(oldKey, null);
            shops.remove(oldShop);
        }

        // Create the new shop entry
        createShop(block, owner, newMaterial, buySell, quantity);
        saveShops();
    }

    private void removeShop(Block block, String owner, Material oldMaterial) {
        Shop oldShop = shops.stream()
                .filter(shop -> shop.getOwner().equals(owner) && shop.getItem().equals(oldMaterial))
                .findFirst()
                .orElse(null);

        if (oldShop != null) {
            // Remove the old shop entry from the configuration file
            String oldKey = owner + "_" + oldShop.getItem().name();
            dataConfig.set(oldKey, null);
            shops.remove(oldShop);
            saveShops();
        }
    }
}