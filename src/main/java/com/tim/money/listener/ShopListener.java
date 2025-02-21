package com.tim.money.listener;

import com.tim.money.Main;
import com.tim.money.manager.PlayerMoneyManager;
import com.tim.money.shop.Shop;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ShopListener implements Listener {
    private final Set<Shop> shops = new HashSet<>();
    private final Set<Block> shopSigns = new HashSet<>();
    PlayerMoneyManager moneyManager = new PlayerMoneyManager();

    public ShopListener(Main main, PlayerMoneyManager playerMoneyManager) {
        this.moneyManager = playerMoneyManager;
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

        if (!(attachedBlock.getState() instanceof Chest) || ((Chest) attachedBlock.getState()).getInventory().getHolder() instanceof DoubleChest) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(0).equalsIgnoreCase(player.getName())) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        Material itemMaterial = Material.getMaterial(event.getLine(1).toUpperCase());
        if (itemMaterial == null) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!event.getLine(2).matches("B\\d+:S\\d+")) {
            if (wasShop) {
                shopSigns.remove(block);
                removeShop(block);
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
                removeShop(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        if (!wasShop) {
            shopSigns.add(block);
            createShop(block, event.getLine(0), itemMaterial, event.getLine(2), quantity);
            player.sendMessage(ChatColor.GREEN + "Shop sign created successfully.");
        } else {
            updateShop(block, event.getLine(0), itemMaterial, event.getLine(2), quantity);
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
            player.sendMessage(String.valueOf(moneyManager.getPlayerMoney(player)));
            moneyManager.removePlayerMoney(player, buyPrice);
            player.sendMessage(String.valueOf(moneyManager.getPlayerMoney(player)));
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

    private void createShop(Block block, String owner, Material itemMaterial, String buySell, int quantity) {
        String[] buySellParts = buySell.split(":");
        double buyPrice = Double.parseDouble(buySellParts[0].substring(1));
        double sellPrice = Double.parseDouble(buySellParts[1].substring(1));
        Shop shop = new Shop(owner, new ItemStack(itemMaterial, quantity), buyPrice, sellPrice, quantity);
        shops.add(shop);
    }

    private void updateShop(Block block, String owner, Material itemMaterial, String buySell, int quantity) {
        removeShop(block);
        createShop(block, owner, itemMaterial, buySell, quantity);
    }

    private void removeShop(Block block) {
        shops.removeIf(shop -> shop.getOwner().equals(block.getLocation().toString()));
    }
}