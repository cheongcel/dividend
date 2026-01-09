package com.eunji.dividend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName; // 회사 이름
    private String ticker;      // 티커 (AAPL)

    // 👇 여기가 범인이었습니다! 깔끔하게 String으로 고쳤습니다.
    private String price;       // 현재 주가

    private String dividend;    // 연 배당금

    // ⭐ 새로 추가된 핵심 필드! (배당 월 정보 저장)
    private String dividendMonths;

    // 생성자
    public DividendEntity(String companyName, String ticker, String price, String dividend, String dividendMonths) {
        this.companyName = companyName;
        this.ticker = ticker;
        this.price = price;
        this.dividend = dividend;
        this.dividendMonths = dividendMonths;
    }
}