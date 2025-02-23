package com.tim.money.listener;

import com.tim.money.manager.PlayerMoneyManager;
import com.tim.money.manager.ShopManager;
import com.tim.money.file.ShopFileHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
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

public class ShopListener implements Listener {
    private final ShopManager shopManager;
    private final ShopFileHandler shopFileHandler;
    private final PlayerMoneyManager moneyManager;

    public ShopListener(JavaPlugin plugin, PlayerMoneyManager playerMoneyManager) {
        this.moneyManager = playerMoneyManager;
        this.shopManager = new ShopManager(plugin);
        this.shopFileHandler = new ShopFileHandler(plugin);
        shopFileHandler.loadShops(shopManager.getShops(), shopManager.getShopSigns());
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

        if (shopManager.getShopSigns().contains(block) && !event.getLine(0).equalsIgnoreCase(player.getName())) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this shop!");
            event.setCancelled(true);
            return;
        }

        Block attachedBlock = getAttachedBlock(block);
        boolean wasShop = shopManager.getShopSigns().contains(block);

        Material oldItemMaterial = null;
        if (wasShop) {
            Sign sign = (Sign) block.getState();
            oldItemMaterial = Material.getMaterial(sign.getLine(1).toUpperCase());
        }

        Material newItemMaterial = Material.getMaterial(event.getLine(1).toUpperCase());
        if (newItemMaterial == null) {
            if (wasShop) {
                shopManager.getShopSigns().remove(block);
                shopManager.removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!(attachedBlock.getState() instanceof Chest) || ((Chest) attachedBlock.getState()).getInventory().getHolder() instanceof DoubleChest) {
            if (wasShop) {
                shopManager.getShopSigns().remove(block);
                shopManager.removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(0).equalsIgnoreCase(player.getName())) {
            if (wasShop) {
                shopManager.getShopSigns().remove(block);
                shopManager.removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(2).matches("B\\d+:S\\d+")) {
            if (wasShop) {
                shopManager.getShopSigns().remove(block);
                shopManager.removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(event.getLine(3));
        } catch (NumberFormatException e) {
            if (wasShop) {
                shopManager.getShopSigns().remove(block);
                shopManager.removeShop(block, event.getLine(0), oldItemMaterial);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!wasShop) {
            shopManager.getShopSigns().add(block);
            shopManager.createShop(block, event.getLine(0), newItemMaterial, event.getLine(2), quantity);
            player.sendMessage(ChatColor.GREEN + "Shop sign created successfully.");
        } else {
            shopManager.updateShop(block, event.getLine(0), oldItemMaterial, newItemMaterial, event.getLine(2), quantity);
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

        if (shopManager.getShopSigns().contains(block) && !player.getName().equalsIgnoreCase(owner) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (!player.getInventory().containsAtLeast(new ItemStack(itemMaterial), quantity)) {
                player.sendMessage(ChatColor.RED + "You don't have enough items to sell!");
                return;
            }
            player.getInventory().removeItem(new ItemStack(itemMaterial, quantity));
            chestInv.addItem(new ItemStack(itemMaterial, quantity));
            moneyManager.addPlayerMoney(player, sellPrice);
            if (ownerPlayer != null) moneyManager.removePlayerMoney(ownerPlayer, sellPrice);
            player.sendMessage(ChatColor.GREEN + "You sold " + quantity + " " + itemMaterial + " for " + sellPrice + "!");

            return;
        }

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

        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (shopManager.getShopSigns().contains(block)) {
            Sign sign = (Sign) block.getState();
            String owner = sign.getLine(0);
            Material itemMaterial = Material.getMaterial(sign.getLine(1).toUpperCase());

            if (itemMaterial != null) {
                shopManager.removeShop(block, owner, itemMaterial);
                event.getPlayer().sendMessage(ChatColor.RED + "Shop sign destroyed and shop entry removed.");
            }
        }
    }
}