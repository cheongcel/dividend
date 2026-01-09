package com.eunji.dividend;

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
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String ticker;
    private String price;
    private String dividend;
    private String dividendMonths;

    public DividendEntity(String companyName, String ticker, String price, String dividend, String dividendMonths) {
        this.companyName = companyName;
        this.ticker = ticker;
        this.price = price;
        this.dividend = dividend;
        this.dividendMonths = dividendMonths;
    }
}