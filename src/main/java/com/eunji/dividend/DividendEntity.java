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

    // [추가된 부분] 배당 월 정보 (예: "4,5,8,11")
    private String dividendMonths;

    public DividendEntity() {
    }

    // [수정된 부분] 5개를 받는 생성자로 변경!
    public DividendEntity(String companyName, String ticker, String price, String dividend, String dividendMonths) {
        this.companyName = companyName;
        this.ticker = ticker;
        this.price = price;
        this.dividend = dividend;
        this.dividendMonths = dividendMonths;
    }

    // Getter들
    public String getCompanyName() { return companyName; }
    public String getTicker() { return ticker; }
    public String getPrice() { return price; }
    public String getDividend() { return dividend; }

    // [추가된 부분]
    public String getDividendMonths() { return dividendMonths; }
}