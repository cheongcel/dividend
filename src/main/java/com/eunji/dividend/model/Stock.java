package com.eunji.dividend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker; // 종목 코드 (ex: 005930.KS)
    private String name;   // 종목 이름 (ex: 삼성전자)

    public Stock(String ticker, String name) {
        this.ticker = ticker;
        this.name = name;
    }
}