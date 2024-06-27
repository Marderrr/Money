package com.tim.money.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerMoneyManager {

    private final Map<Player, MoneyManager> playerMoneyMap;

    public PlayerMoneyManager() {
        this.playerMoneyMap = new HashMap<>();
    }

    public MoneyManager getMoneyManager(Player player) {
        return playerMoneyMap.get(player);
    }

    public void setMoneyManager(Player player, MoneyManager moneyManager) {
        this.playerMoneyMap.put(player, moneyManager);
    }

    public void addMoney(Player player, double amount) {
        MoneyManager moneyManager = getMoneyManager(player);
        if (moneyManager != null) {
            moneyManager.addMoney(amount);
        }
    }

    public void removeMoney(Player player, double amount) {
        MoneyManager moneyManager = getMoneyManager(player);
        if (moneyManager != null) {
            moneyManager.removeMoney(amount);
        }
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        MoneyManager moneyManager = getMoneyManager(player);
        return moneyManager != null && moneyManager.hasEnoughMoney(amount);
    }

    public void setMoney(Player player, double amount) {
        MoneyManager moneyManager = getMoneyManager(player);
        if (moneyManager != null) {
            moneyManager.setMoney(amount);
        }
    }

    public double getMoney(Player player) {
        MoneyManager moneyManager = getMoneyManager(player);
        if (moneyManager != null) {
            return moneyManager.getMoney();
        }
        return 0;
    }
}