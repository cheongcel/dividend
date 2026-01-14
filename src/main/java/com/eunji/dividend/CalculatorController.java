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
import java.text.DecimalFormat;
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

    // 1. 계산만 하기 (저장 X)
    @PostMapping("/calculate")
    public String calculate(@RequestParam("ticker") String ticker, @RequestParam("quantity") int quantity, Model model) {
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

    // 2. 추가하기 (저장 O)
    @PostMapping("/add")
    public String addStock(@RequestParam("ticker") String ticker, @RequestParam("quantity") int quantity) {
        dividendService.calculateDividend(ticker, quantity, true);
        return "redirect:/calendar";
    }

    // 3. 삭제하기
    @PostMapping("/delete")
    public String deleteStock(@RequestParam("ticker") String ticker) {
        UserPortfolio portfolio = userPortfolioRepository.findByTicker(ticker);
        if (portfolio != null) {
            userPortfolioRepository.delete(portfolio);
        }
        return "redirect:/calendar";
    }

    // 4. 캘린더 보여주기
    @GetMapping("/calendar")
    public String showCalendar(Model model) {
        // 1. 빈 달력 틀 만들기
        List<BigDecimal> monthlyTotals = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
        List<List<String>> monthlyDetails = new ArrayList<>();
        for (int i = 0; i < 12; i++) monthlyDetails.add(new ArrayList<>());

        // 2. 내 주식 가져오기
        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();
        BigDecimal totalAnnual = BigDecimal.ZERO;

        // 숫자 꾸미기용 (1,000)
        DecimalFormat df = new DecimalFormat("#,###");

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());

            if (info == null || info.getDividend() == null || info.getDividend().equals("null")) continue;

            try {
                BigDecimal annualPerShare = new BigDecimal(info.getDividend());
                BigDecimal quantity = new BigDecimal(stock.getQuantity());

                BigDecimal myTotalAnnual = annualPerShare.multiply(quantity);
                totalAnnual = totalAnnual.add(myTotalAnnual);

                String monthsStr = info.getDividendMonths();
                String[] months;
                if (monthsStr == null || monthsStr.isEmpty()) {
                    months = new String[]{};
                } else {
                    months = monthsStr.split(",");
                }

                if (months.length > 0) {
                    BigDecimal splitAmount = myTotalAnnual.divide(new BigDecimal(months.length), 0, RoundingMode.HALF_UP);

                    for (String m : months) {
                        try {
                            int monthIdx = Integer.parseInt(m.trim()) - 1;
                            if (monthIdx >= 0 && monthIdx < 12) {
                                monthlyTotals.set(monthIdx, monthlyTotals.get(monthIdx).add(splitAmount));
                                String detailText = info.getCompanyName() + ": " + df.format(splitAmount) + "원";
                                monthlyDetails.get(monthIdx).add(detailText);
                            }
                        } catch (Exception e) {}
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ 캘린더 계산 에러 (" + stock.getTicker() + "): " + e.getMessage());
            }
        }

        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("monthlyDetails", monthlyDetails);
        model.addAttribute("myStocks", myStocks); // 내 주식 목록도 전달

        return "calendar";
    }

    // 5. ⭐ [핵심 수정] 파이어족 목표 페이지 (에러 해결됨)
    @GetMapping("/goal")
    public String showGoal(@RequestParam(value = "targetMonthly", required = false, defaultValue = "0") int targetMonthly, Model model) {

        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();
        BigDecimal currentAnnualDividend = BigDecimal.ZERO;

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());
            if (info != null && info.getDividend() != null && !info.getDividend().equals("null")) {
                try {
                    BigDecimal stockTotal = new BigDecimal(info.getDividend()).multiply(new BigDecimal(stock.getQuantity()));
                    currentAnnualDividend = currentAnnualDividend.add(stockTotal);
                } catch (Exception e) {}
            }
        }

        model.addAttribute("targetMonthly", targetMonthly);
        model.addAttribute("currentAnnual", currentAnnualDividend);

        if (targetMonthly == 0) {
            model.addAttribute("progressPercent", 0);
            return "goal";
        }

        // 1. 목표 금액 계산 (단위: 만원 -> 원)
        BigDecimal realTargetMonthly = new BigDecimal(targetMonthly).multiply(new BigDecimal("10000"));
        BigDecimal targetAnnual = realTargetMonthly.multiply(new BigDecimal(12));

        BigDecimal gap = targetAnnual.subtract(currentAnnualDividend);

        // 2. 달성률 계산
        double percent = 0.0;
        if (targetAnnual.compareTo(BigDecimal.ZERO) > 0) {
            percent = currentAnnualDividend.doubleValue() / targetAnnual.doubleValue() * 100;
        }
        if (percent > 100) percent = 100;

        // 3. 삼성전자 환산
        BigDecimal samsungDividend = new BigDecimal("1444");
        BigDecimal neededShares = BigDecimal.ZERO;
        BigDecimal gapAmount = BigDecimal.ZERO;

        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            gapAmount = gap;
            neededShares = gap.divide(samsungDividend, 0, RoundingMode.UP);
        }

        // 4. ⭐ [추가됨] HTML에서 에러나지 않도록 여기서 미리 월 금액을 계산해서 보냅니다!
        BigDecimal currentMonthly = currentAnnualDividend.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
        BigDecimal gapMonthly = BigDecimal.ZERO;
        if (gapAmount.compareTo(BigDecimal.ZERO) > 0) {
            gapMonthly = gapAmount.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
        }

        // 5. 모델에 담기
        model.addAttribute("progressPercent", String.format("%.1f", percent));
        model.addAttribute("neededShares", neededShares);
        model.addAttribute("gapAmount", gapAmount);

        // 새로 만든 월 단위 변수들도 전달
        model.addAttribute("currentMonthly", currentMonthly);
        model.addAttribute("gapMonthly", gapMonthly);

        return "goal";
    }
}