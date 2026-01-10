package com.eunji.dividend;

import com.eunji.dividend.service.DividendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class CalculatorController {

    private final DividendService dividendService;
    private final DividendRepository dividendRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    @GetMapping("/")
    public String showMain() { return "index"; }

    @GetMapping("/calculator")
    public String showCalculator() { return "calculator"; }

    // 1. 계산만 하기 (저장 X) -> false를 꼭 보내야 함!
    @PostMapping("/calculate")
    public String calculate(@RequestParam("ticker") String ticker, @RequestParam("quantity") int quantity, Model model) {
        // ⭐ false: 저장 안 함
        Map<String, Object> result = dividendService.calculateDividend(ticker, quantity, false);

        if (result.containsKey("error")) {
            model.addAttribute("error", result.get("error"));
        } else {
            model.addAttribute("result", result);
            model.addAttribute("searchTicker", ticker);
            model.addAttribute("searchQuantity", quantity);
        }
        return "calculator";
    }

    // 2. 추가하기 (저장 O) -> true 보내기
    @PostMapping("/add")
    public String addStock(@RequestParam("ticker") String ticker, @RequestParam("quantity") int quantity) {
        // ⭐ true: 저장 함
        dividendService.calculateDividend(ticker, quantity, true);
        return "redirect:/calendar";
    }

    // (아래 캘린더, 목표 코드는 그대로 유지)
    @GetMapping("/calendar")
    public String showCalendar(Model model) {
        // ... (기존 코드 유지)
        return "calendar";
    }

    @GetMapping("/goal")
    public String showGoal() { return "goal"; }
}