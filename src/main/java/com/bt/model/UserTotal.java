package com.bt.model;

public class UserTotal {
    private String username;
    private double total;

    public UserTotal(String username, double total) {
        this.username = username;
        this.total = total;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getTotal() {
        return total;
    }
}
