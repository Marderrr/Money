package com.tim.money.shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.HashSet;
import java.util.Set;

public class ShopSignListener implements Listener {
    private final Set<Block> shopSigns = new HashSet<>();
    private boolean standing;

    private Block getAttachedBlock(Block signBlock) {
        if (signBlock.getBlockData() instanceof WallSign) {
            return signBlock.getRelative(((WallSign) signBlock.getBlockData()).getFacing().getOppositeFace());
        } else {
            // For floor signs, the attached block is directly below the sign
            return signBlock.getRelative(BlockFace.DOWN);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Block attachedBlock = getAttachedBlock(block);
        boolean wasShop = shopSigns.contains(block);

        // Only proceed if the attached block is a chest
        if (!(attachedBlock.getState() instanceof Chest) || ((Chest) attachedBlock.getState()).getInventory().getHolder() instanceof DoubleChest) {
            if (wasShop) {
                shopSigns.remove(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        // Check if the first line is the player's name
        if (!lines[0].equalsIgnoreCase(player.getName())) {
            if (wasShop) {
                shopSigns.remove(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        // Check if the second line is a valid Minecraft item name
        Material itemMaterial = Material.getMaterial(lines[1].toUpperCase());
        if (itemMaterial == null) {
            if (wasShop) {
                shopSigns.remove(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        // Check if the third line matches the buy/sell format
        if (!lines[2].matches("B\\d+:S\\d+")) {
            if (wasShop) {
                shopSigns.remove(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        // Check if the fourth line is a valid number
        try {
            Integer.parseInt(lines[3]);
        } catch (NumberFormatException e) {
            if (wasShop) {
                shopSigns.remove(block);
                player.sendMessage(ChatColor.RED + "This sign is no longer a shop.");
            }
            return;
        }

        // If all checks pass, add the sign to the shopSigns set
        if (!wasShop) {
            shopSigns.add(block);
            player.sendMessage(ChatColor.GREEN + "Shop sign created successfully.");
        }
    }
}