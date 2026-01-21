package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    // ⭐ 추가: 특정 사용자의 모든 포트폴리오 조회
    List<UserPortfolio> findByUserId(Long userId);

    // ⭐ 추가: 특정 사용자의 특정 종목 찾기
    UserPortfolio findByUserIdAndTicker(Long userId, String ticker);

    // ⚠️ 기존 메서드는 주석 처리 (userId 없이는 못 찾음)
    // UserPortfolio findByTicker(String ticker);
}