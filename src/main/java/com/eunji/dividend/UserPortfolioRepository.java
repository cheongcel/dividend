package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {
    UserPortfolio findByTicker(String ticker);
}