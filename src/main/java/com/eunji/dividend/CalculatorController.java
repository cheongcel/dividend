package com.eunji.dividend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CalculatorController {

    @Autowired
    private DividendRepository dividendRepository;

    @GetMapping("/")
    public String showMain() { return "index"; }

    @GetMapping("/calculator")
    public String showCalculator() { return "calculator"; }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam("ticker") String ticker,
            @RequestParam("quantity") int quantity,
            Model model
    ) {
        ticker = ticker.trim().toUpperCase(); // 대문자로 변환
        String name = "";
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal monthlyDividend = BigDecimal.ZERO;

        try {
            // 1. [캐시 확인] DB에 저장된 게 있는지 먼저 본다
            DividendEntity existingData = dividendRepository.findByTicker(ticker);

            if (existingData != null) {
                System.out.println("⚡ [캐시 적중] DB 사용: " + existingData.getCompanyName());
                name = existingData.getCompanyName();
                price = new BigDecimal(existingData.getPrice());
                monthlyDividend = new BigDecimal(existingData.getDividend());
            } else {
                System.out.println("🛠️ [시뮬레이션 모드] 가상 데이터 생성: " + ticker);

                // 🎰 시뮬레이션 엔진: 종목에 맞는 현실적인 데이터 생성
                if (ticker.endsWith(".KS")) {
                    if (ticker.contains("005930")) {
                        name = "Samsung Electronics";
                        price = new BigDecimal("75800");
                        monthlyDividend = new BigDecimal("120").multiply(new BigDecimal(quantity));
                    } else if (ticker.contains("005380")) {
                        name = "Hyundai Motor";
                        price = new BigDecimal("245000");
                        monthlyDividend = new BigDecimal("800").multiply(new BigDecimal(quantity));
                    } else {
                        name = "Korea Stock (" + ticker + ")";
                        price = new BigDecimal("50000");
                        monthlyDividend = new BigDecimal("100").multiply(new BigDecimal(quantity));
                    }
                } else {
                    if (ticker.equals("AAPL")) {
                        name = "Apple Inc.";
                        price = new BigDecimal("286000"); // 환율 적용
                        monthlyDividend = new BigDecimal("120").multiply(new BigDecimal(quantity));
                    } else if (ticker.equals("O")) {
                        name = "Realty Income";
                        price = new BigDecimal("78650");
                        monthlyDividend = new BigDecimal("357").multiply(new BigDecimal(quantity));
                    } else if (ticker.equals("TSLA")) {
                        name = "Tesla, Inc.";
                        price = new BigDecimal("357500");
                        monthlyDividend = BigDecimal.ZERO;
                    } else if (ticker.equals("MSFT")) {
                        name = "Microsoft Corp";
                        price = new BigDecimal("572000");
                        monthlyDividend = new BigDecimal("360").multiply(new BigDecimal(quantity));
                    } else {
                        name = "US Stock (" + ticker + ")";
                        price = new BigDecimal("143000");
                        monthlyDividend = new BigDecimal("200").multiply(new BigDecimal(quantity));
                    }
                }

                // 2. DB 저장
                DividendEntity entity = new DividendEntity(
                        name, ticker, price.toString(), monthlyDividend.toString()
                );
                dividendRepository.save(entity);
                System.out.println("✅ DB 저장 완료: " + name);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "오류 발생: " + e.getMessage());
            return "calculator";
        }

        model.addAttribute("result", monthlyDividend.toBigInteger());
        model.addAttribute("companyName", name);
        model.addAttribute("price", price.toBigInteger());

        return "calculator";
    }

    @GetMapping("/calendar")
    public String showCalendar() { return "calendar"; }

    @GetMapping("/chart")
    public String showChart() { return "chart"; }
}