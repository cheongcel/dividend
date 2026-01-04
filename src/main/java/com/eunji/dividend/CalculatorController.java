package com.eunji.dividend;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalculatorController {

    // 1. 메인 화면 보여주기
    @GetMapping("/")
    public String showMain() {
        return "index";
    }

    // 2. 배당 계산기 화면 보여주기
    @GetMapping("/calculator")
    public String showCalculator() {
        return "calculator";
    }

    // 2-1. 배당금 계산 로직 처리
    @PostMapping("/calculate")
    public String calculate(
            @RequestParam("ticker") String ticker,
            @RequestParam("quantity") int quantity,
            Model model
    ) {
        // 가짜 데이터 로직
        int dividendPerShare = 361;
        int totalDividend = dividendPerShare * quantity;

        model.addAttribute("result", totalDividend);
        return "calculator";
    }

    // 3. 배당 캘린더 화면 (추가됨!)
    @GetMapping("/calendar")
    public String showCalendar() {
        return "calendar"; // calendar.html 파일 필요함
    }

    // 4. 성장률 차트 화면 (추가됨!)
    @GetMapping("/chart")
    public String showChart() {
        return "chart"; // chart.html 파일 필요함
    }
}