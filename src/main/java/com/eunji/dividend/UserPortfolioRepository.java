package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {
    // 내 통장에 이미 이 주식이 있는지 확인하는 기능 (예: 'AAPL' 있니?)
    UserPortfolio findByTicker(String ticker);
}