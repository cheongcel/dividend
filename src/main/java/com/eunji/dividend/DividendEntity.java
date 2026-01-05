package com.eunji.dividend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String ticker;
    private String price;
    private String dividend;

    // 1. 기본 생성자 (JPA 필수)
    public DividendEntity() {
    }

    // 2. 데이터를 담을 때 쓰는 생성자
    public DividendEntity(String companyName, String ticker, String price, String dividend) {
        this.companyName = companyName;
        this.ticker = ticker;
        this.price = price;
        this.dividend = dividend;
    }

    // ==========================================
    // [중요] 컨트롤러가 데이터를 꺼내갈 수 있게 해주는 Getter들
    // ==========================================

    public String getCompanyName() {
        return companyName;
    }

    public String getTicker() {
        return ticker;
    }

    public String getPrice() {
        return price;
    }

    public String getDividend() {
        return dividend;
    }
}