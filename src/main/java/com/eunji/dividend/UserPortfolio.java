package com.eunji.dividend;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "user_portfolio")
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐ 추가: 어느 사용자의 포트폴리오인지
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private int quantity;

    // ⭐ 기존 생성자 수정 (userId 추가)
    public UserPortfolio(Long userId, String ticker, int quantity) {
        this.userId = userId;
        this.ticker = ticker;
        this.quantity = quantity;
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }
}