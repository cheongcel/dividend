package com.eunji.dividend.controller;

import com.eunji.dividend.model.Stock;
import com.eunji.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockApiController {

    private final StockRepository stockRepository;

    @GetMapping("/api/autocomplete")
    public List<Stock> autocomplete(@RequestParam String keyword) {
        // 검색어가 없으면 빈 리스트 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        // DB에서 검색해서 반환
        return stockRepository.findByNameContainingIgnoreCase(keyword);
    }
}