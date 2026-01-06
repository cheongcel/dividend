package com.eunji.dividend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CalculatorController {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private UserPortfolioRepository userPortfolioRepository;

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
            DividendEntity existingData = dividendRepository.findByTicker(ticker);

            if (existingData != null) {
                name = existingData.getCompanyName();
                price = new BigDecimal(existingData.getPrice());
            } else {
                System.out.println("🛠️ [시뮬레이션 모드] 가상 데이터 생성: " + ticker);

                String months = "";
                BigDecimal annualDividend = BigDecimal.ZERO;

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
                price = new BigDecimal(entity.getPrice());
            }

            UserPortfolio myStock = userPortfolioRepository.findByTicker(ticker);

            if (myStock != null) {
                myStock.addQuantity(quantity);
                userPortfolioRepository.save(myStock);
            } else {
                myStock = new UserPortfolio(ticker, quantity);
                userPortfolioRepository.save(myStock);
            }

            BigDecimal annualDiv = new BigDecimal(existingData.getDividend());
            BigDecimal totalQty = new BigDecimal(myStock.getQuantity());

            monthlyDividend = annualDiv
                    .multiply(totalQty)
                    .divide(new BigDecimal(12), 0, BigDecimal.ROUND_HALF_UP);

            name = existingData.getCompanyName();

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

    // [캘린더 섹션] 월별 배당금 계산 로직
    @GetMapping("/calendar")
    public String showCalendar(Model model) {

        List<BigDecimal> monthlyTotals = new ArrayList<>();
        List<List<String>> monthlyDetails = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            monthlyTotals.add(BigDecimal.ZERO);
            monthlyDetails.add(new ArrayList<>());
        }

        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();
        BigDecimal totalAnnual = BigDecimal.ZERO;

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());

            if (info != null && info.getDividendMonths() != null && !info.getDividendMonths().isEmpty()) {

                BigDecimal annualTotal = new BigDecimal(info.getDividend())
                        .multiply(new BigDecimal(stock.getQuantity()));

                totalAnnual = totalAnnual.add(annualTotal);

                String monthsStr = info.getDividendMonths();
                String[] months;

                if (monthsStr.equals("Monthly")) {
                    months = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12"};
                } else {
                    months = monthsStr.split(",");
                }

                BigDecimal oneTimePay = annualTotal.divide(new BigDecimal(months.length), 0, BigDecimal.ROUND_HALF_UP);

                for (String m : months) {
                    try {
                        int monthIdx = Integer.parseInt(m.trim()) - 1;
                        BigDecimal currentTotal = monthlyTotals.get(monthIdx);
                        monthlyTotals.set(monthIdx, currentTotal.add(oneTimePay));

                        String detail = info.getCompanyName() + ": " + oneTimePay + "원";
                        monthlyDetails.get(monthIdx).add(detail);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }

        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("monthlyDetails", monthlyDetails);

        return "calendar";
    }

    @GetMapping("/chart")
    public String showChart() { return "chart"; }

    // [목표 섹션]
    @GetMapping("/goal")
    public String showGoal(@RequestParam(value = "targetMonthly", defaultValue = "0") int targetMonthly, Model model) {

        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();
        BigDecimal currentAnnualDividend = BigDecimal.ZERO;

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());
            if (info != null) {
                BigDecimal stockTotal = new BigDecimal(info.getDividend())
                        .multiply(new BigDecimal(stock.getQuantity()));
                currentAnnualDividend = currentAnnualDividend.add(stockTotal);
            }
        }

        if (targetMonthly == 0) {
            model.addAttribute("currentAnnual", currentAnnualDividend);
            model.addAttribute("progressPercent", 0);
            return "goal";
        }

        BigDecimal targetAnnual = new BigDecimal(targetMonthly).multiply(new BigDecimal(12));
        BigDecimal gap = targetAnnual.subtract(currentAnnualDividend);

        double percent = 0.0;
        if (targetAnnual.compareTo(BigDecimal.ZERO) > 0) {
            percent = currentAnnualDividend.doubleValue() / targetAnnual.doubleValue() * 100;
        }
        if (percent > 100) percent = 100;

        BigDecimal samsungDiv = new BigDecimal("1444");
        BigDecimal samsungPrice = new BigDecimal("75800");

        BigDecimal neededShares = BigDecimal.ZERO;
        BigDecimal neededMoney = BigDecimal.ZERO;

        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            neededShares = gap.divide(samsungDiv, 0, BigDecimal.ROUND_UP);
            neededMoney = neededShares.multiply(samsungPrice);
        }

        model.addAttribute("targetMonthly", targetMonthly);
        model.addAttribute("currentAnnual", currentAnnualDividend);
        model.addAttribute("progressPercent", String.format("%.1f", percent));
        model.addAttribute("neededShares", neededShares);
        model.addAttribute("neededMoney", neededMoney);

        return "goal";
    }
}