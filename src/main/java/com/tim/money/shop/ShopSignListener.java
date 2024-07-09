package com.tim.money.shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ShopSignListener implements Listener {
    private final Set<Block> shopSigns = new HashSet<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!(event.getBlockPlaced().getState() instanceof Sign)) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Example logic: Check if the player is holding a specific item to determine intention
        // This is just an example. Adjust according to your actual criteria
        if (itemInHand.getType() == Material.DIAMOND) {
            shopSigns.add(event.getBlockPlaced());
            player.sendMessage(ChatColor.RED + "Shop sign placed. This sign is now unchangeable.");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (shopSigns.contains(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This shop sign cannot be changed.");
        }
    }
}