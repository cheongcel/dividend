package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendRepository extends JpaRepository<DividendEntity, Long> {
    DividendEntity findByTicker(String ticker);
}