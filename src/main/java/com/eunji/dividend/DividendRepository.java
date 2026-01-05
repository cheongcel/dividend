package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DividendRepository extends JpaRepository<DividendEntity, Long> {
    // [추가됨] 티커(예: AAPL)로 저장된 게 있는지 찾는 기능
    DividendEntity findByTicker(String ticker);
}