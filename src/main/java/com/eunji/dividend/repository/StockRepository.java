package com.eunji.dividend.repository;

import com.eunji.dividend.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// public class가 아니라 public interface여야 합니다!
public interface StockRepository extends JpaRepository<Stock, Long> {

    // "이름에 글자가 포함된 거 다 찾아와!" (대소문자 무시)
    List<Stock> findByNameContainingIgnoreCase(String keyword);
}