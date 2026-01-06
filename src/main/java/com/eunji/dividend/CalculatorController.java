package com.eunji.dividend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List; // 리스트 사용을 위해 추가

@Controller
public class CalculatorController {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository; // [중요] 내 통장 관리인 추가

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
        ticker = ticker.trim().toUpperCase();
        String name = "";
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal monthlyDividend = BigDecimal.ZERO;

        try {
            // 1. [시장 정보] 가져오기
            DividendEntity existingData = dividendRepository.findByTicker(ticker);

            if (existingData != null) {
                System.out.println("⚡ [캐시 적중] DB 사용: " + existingData.getCompanyName());
                name = existingData.getCompanyName();
                price = new BigDecimal(existingData.getPrice());
            } else {
                System.out.println("🛠️ [시뮬레이션 모드] 가상 데이터 생성: " + ticker);

                String months = "";
                BigDecimal annualDividend = BigDecimal.ZERO;

                // 시뮬레이션 엔진
                if (ticker.endsWith(".KS")) {
                    if (ticker.contains("005930")) {
                        name = "Samsung Electronics";
                        price = new BigDecimal("75800");
                        annualDividend = new BigDecimal("1444");
                        months = "4,5,8,11";
                    } else if (ticker.contains("005380")) {
                        name = "Hyundai Motor";
                        price = new BigDecimal("245000");
                        annualDividend = new BigDecimal("11000");
                        months = "4,8";
                    } else {
                        name = "Korea Stock (" + ticker + ")";
                        price = new BigDecimal("50000");
                        annualDividend = new BigDecimal("1500");
                        months = "4";
                    }
                } else {
                    if (ticker.equals("AAPL")) {
                        name = "Apple Inc.";
                        price = new BigDecimal("286000");
                        annualDividend = new BigDecimal("1400");
                        months = "2,5,8,11";
                    } else if (ticker.equals("O")) {
                        name = "Realty Income";
                        price = new BigDecimal("78650");
                        annualDividend = new BigDecimal("4300");
                        months = "Monthly";
                    } else {
                        name = "US Stock (" + ticker + ")";
                        price = new BigDecimal("143000");
                        annualDividend = new BigDecimal("2000");
                        months = "1,4,7,10";
                    }
                }

                DividendEntity entity = new DividendEntity(
                        name, ticker, price.toString(), annualDividend.toString(), months
                );
                dividendRepository.save(entity);
                existingData = entity;
                price = new BigDecimal(entity.getPrice()); // 가격 업데이트
            }

            // 2. [내 통장] 포트폴리오 저장/업데이트
            UserPortfolio myStock = userPortfolioRepository.findByTicker(ticker);

            if (myStock != null) {
                System.out.println("💰 [포트폴리오] 기존 수량에 추가: " + quantity + "주");
                myStock.addQuantity(quantity);
                userPortfolioRepository.save(myStock);
            } else {
                System.out.println("🆕 [포트폴리오] 신규 종목 추가: " + quantity + "주");
                myStock = new UserPortfolio(ticker, quantity);
                userPortfolioRepository.save(myStock);
            }

            // 3. 화면 출력 (월급 계산)
            BigDecimal annualDiv = new BigDecimal(existingData.getDividend());
            BigDecimal totalQty = new BigDecimal(myStock.getQuantity());

            monthlyDividend = annualDiv
                    .multiply(totalQty)
                    .divide(new BigDecimal(12), 0, BigDecimal.ROUND_HALF_UP);

            name = existingData.getCompanyName(); // 이름 확실히 하기

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
    public String showChart() { return "chart"; } // [수정됨] 여기에 닫는 괄호 } 추가 완료!

    // [목표 섹션] 보여주는 화면
    @GetMapping("/goal")
    public String showGoal(@RequestParam(value = "targetMonthly", defaultValue = "0") int targetMonthly, Model model) {

        // 1. 내 포트폴리오 다 가져오기
        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();

        // 2. 현재 나의 연 배당금 총액 계산
        BigDecimal currentAnnualDividend = BigDecimal.ZERO;

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());
            if (info != null) {
                // (내 보유수량 * 주당 연배당금)
                BigDecimal stockTotal = new BigDecimal(info.getDividend())
                        .multiply(new BigDecimal(stock.getQuantity()));
                currentAnnualDividend = currentAnnualDividend.add(stockTotal);
            }
        }

        // 3. 목표 계산
        if (targetMonthly == 0) {
            model.addAttribute("currentAnnual", currentAnnualDividend);
            model.addAttribute("progressPercent", 0);
            return "goal";
        }

        // 4. 목표 분석
        BigDecimal targetAnnual = new BigDecimal(targetMonthly).multiply(new BigDecimal(12));
        BigDecimal gap = targetAnnual.subtract(currentAnnualDividend);

        double percent = 0.0;
        if (targetAnnual.compareTo(BigDecimal.ZERO) > 0) {
            percent = currentAnnualDividend.doubleValue() / targetAnnual.doubleValue() * 100;
        }
        if (percent > 100) percent = 100;

        // 삼성전자 기준 계산
        BigDecimal samsungDiv = new BigDecimal("1444");
        BigDecimal samsungPrice = new BigDecimal("75800");

        BigDecimal neededShares = BigDecimal.ZERO;
        BigDecimal neededMoney = BigDecimal.ZERO;

        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            neededShares = gap.divide(samsungDiv, 0, BigDecimal.ROUND_UP);
            neededMoney = neededShares.multiply(samsungPrice);
        }

        // 5. 화면 전송
        model.addAttribute("targetMonthly", targetMonthly);
        model.addAttribute("currentAnnual", currentAnnualDividend);
        model.addAttribute("progressPercent", String.format("%.1f", percent));
        model.addAttribute("neededShares", neededShares);
        model.addAttribute("neededMoney", neededMoney);

        return "goal";
    }
}