package com.eunji.dividend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;   // 종목 코드
    private int quantity;    // 수량

    public UserPortfolio() {
    }

    public UserPortfolio(String ticker, int quantity) {
        this.ticker = ticker;
        this.quantity = quantity;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public String getTicker() { return ticker; }
    public int getQuantity() { return quantity; }
}