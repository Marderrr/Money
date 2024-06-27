package com.tim.money.manager;

public class MoneyManager {

    private double money;

    public MoneyManager(double money) {
        this.money = money;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double amount) {
        this.money = amount;
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    public void removeMoney(double amount) {
        this.money -= amount;
    }

    public boolean hasEnoughMoney(double amount) {
        return this.money >= amount;
    }

}
