package com.eunji.dividend;

import com.eunji.dividend.service.DividendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CalculatorController {

    private final DividendService dividendService;

    @GetMapping("/")
    public String showMain() {
        return "index";
    }

    @GetMapping("/calculator")
    public String showCalculator() {
        return "calculator";
    }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam("ticker") String ticker,
            @RequestParam("quantity") int quantity,
            Model model
    ) {
        // Service에게 계산 맡기기
        Map<String, Object> result = dividendService.calculateDividend(ticker, quantity);

        // 에러가 있으면 에러만 보내기
        if (result.containsKey("error")) {
            model.addAttribute("error", result.get("error"));
            return "calculator";
        }

        // 성공하면 result Map 통째로 보내기
        model.addAttribute("result", result);

        return "calculator";
    }

    @GetMapping("/calendar")
    public String showCalendar() {
        return "calendar";
    }

    @GetMapping("/goal")
    public String showGoal() {
        return "goal";
    }
}