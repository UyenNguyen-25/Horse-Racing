package model;

import java.io.Serializable;

public class Account implements Serializable {
    private String username;
    private String password;
    private int balance;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 100;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}