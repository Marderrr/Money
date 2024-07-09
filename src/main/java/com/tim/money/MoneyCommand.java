package com.tim.money;

import com.tim.money.manager.PlayerMoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

    private PlayerMoneyManager playerMoneyManager;

    public MoneyCommand(PlayerMoneyManager playerMoneyManager) {
        this.playerMoneyManager = playerMoneyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "Geld: "  + ChatColor.GREEN + "" + ChatColor.BOLD + playerMoneyManager.getPlayerMoney(player));
            return true;
        }

        String action = args[0].toLowerCase();
        Player targetPlayer = null;
        double amount = 0;

        if (action.equals("get")) {
            if (args.length > 1) {
                targetPlayer = Bukkit.getPlayer(args[1]);
            }
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
                return true;
            }
            double money = playerMoneyManager.getPlayerMoney(targetPlayer);
            player.sendMessage(ChatColor.GRAY + "Geld von " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY + ": " + ChatColor.GREEN + "" + ChatColor.BOLD + money);
            return true;
        } else {
            if (args.length > 1) {
                targetPlayer = Bukkit.getPlayer(args[1]);
            }
            if (args.length > 2) {
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Der Betrag muss eine Zahl sein!");
                    return true;
                }
            }
        }

        switch (action) {
            case "send":
                if (targetPlayer == null) {
                    player.sendMessage("" + ChatColor.RED + "" + ChatColor.BOLD + args[1] + ChatColor.RESET + ChatColor.RED + " wurde nicht gefunden!");
                } else if (!playerMoneyManager.hasEnoughPlayerMoney(player, amount)) {
                    player.sendMessage(ChatColor.RED + "Nicht genug Geld! Dein aktuelles Geld: " + ChatColor.GREEN + "" + ChatColor.BOLD + playerMoneyManager.getPlayerMoney(player));
                } else {
                    playerMoneyManager.removePlayerMoney(player, amount);
                    playerMoneyManager.addPlayerMoney(targetPlayer, amount);
                    player.sendMessage(ChatColor.GRAY + "Du hast " + ChatColor.GREEN + "" + ChatColor.BOLD + amount + ChatColor.RESET + ChatColor.GRAY  + " an " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY  + " gesendet");
                }
                break;

            case "set":
                if (targetPlayer != null) {
                    playerMoneyManager.setPlayerMoney(targetPlayer, amount);
                    player.sendMessage(ChatColor.GRAY + "Geld von " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY + " auf " + ChatColor.GREEN + "" + ChatColor.BOLD + amount + ChatColor.RESET + ChatColor.GRAY + " gesetzt");
                } else {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
                }
                break;

            case "add":
                if (targetPlayer != null) {
                    playerMoneyManager.addPlayerMoney(targetPlayer, amount);
                    player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + amount + ChatColor.RESET + ChatColor.GRAY  + " zu " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY  + " hinzugefügt!");
                } else {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
                }
                break;

            case "remove":
                if (targetPlayer != null) {
                    playerMoneyManager.removePlayerMoney(targetPlayer, amount);
                    player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + amount + ChatColor.RESET + ChatColor.GRAY  + " von " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY  + " entfernt!");
                } else {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
                }
                break;

            case "get":
                if (targetPlayer != null) {
                    double money = playerMoneyManager.getPlayerMoney(targetPlayer);
                    player.sendMessage(ChatColor.GRAY + "Geld von " + ChatColor.GOLD + "" + ChatColor.BOLD + targetPlayer.getName() + ChatColor.RESET + ChatColor.GRAY + ": " + ChatColor.GREEN + "" + ChatColor.BOLD + money);
                } else {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
                }

            default:
                player.sendMessage(ChatColor.RED + "Unbekannter Befehl! Benutze: /money [send | set | add | remove | get] [Spieler] [Betrag]");
                break;
        }

        return true;
    }
}