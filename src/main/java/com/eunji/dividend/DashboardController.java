package com.eunji.dividend;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DividendRepository dividendRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/";
        }

        List<UserPortfolio> myStocks = userPortfolioRepository.findByUserId(userId);

        List<BigDecimal> monthlyTotals = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
        BigDecimal totalAnnual = BigDecimal.ZERO;

        DecimalFormat df = new DecimalFormat("#,###");

        List<Map<String, Object>> stockDetails = new ArrayList<>();

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());
            if (info == null) continue;

            try {
                BigDecimal annualPerShare = info.getDividend() != null && !info.getDividend().equals("null")
                        ? new BigDecimal(info.getDividend()) : BigDecimal.ZERO;
                BigDecimal quantity = new BigDecimal(stock.getQuantity());
                BigDecimal myTotalAnnual = annualPerShare.multiply(quantity);
                totalAnnual = totalAnnual.add(myTotalAnnual);

                BigDecimal price = info.getPrice() != null && !info.getPrice().equals("null")
                        ? new BigDecimal(info.getPrice()) : BigDecimal.ZERO;
                BigDecimal assetValue = price.multiply(quantity);

                String monthsStr = info.getDividendMonths();
                if (monthsStr != null && !monthsStr.isEmpty()) {
                    String[] months = monthsStr.split(",");
                    if (months.length > 0) {
                        BigDecimal splitAmount = myTotalAnnual.divide(new BigDecimal(months.length), 0, RoundingMode.HALF_UP);
                        for (String m : months) {
                            try {
                                int monthIdx = Integer.parseInt(m.trim()) - 1;
                                if (monthIdx >= 0 && monthIdx < 12) {
                                    monthlyTotals.set(monthIdx, monthlyTotals.get(monthIdx).add(splitAmount));
                                }
                            } catch (Exception e) {}
                        }
                    }
                }

                Map<String, Object> stockDetail = new HashMap<>();
                stockDetail.put("ticker", stock.getTicker());
                stockDetail.put("name", info.getCompanyName());
                stockDetail.put("quantity", stock.getQuantity());
                stockDetail.put("price", price);
                stockDetail.put("assetValue", assetValue);
                stockDetail.put("annualDividend", myTotalAnnual);
                stockDetail.put("dividendYield",
                        assetValue.compareTo(BigDecimal.ZERO) > 0
                                ? myTotalAnnual.divide(assetValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                                : BigDecimal.ZERO
                );
                stockDetails.add(stockDetail);

            } catch (Exception e) {
                System.out.println("⚠️ 대시보드 계산 에러: " + e.getMessage());
            }
        }

        BigDecimal monthlyAvg = totalAnnual.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
        int stockCount = myStocks.size();

        String investmentStatus;
        if (stockCount == 0) investmentStatus = "주식을 추가해보세요";
        else if (stockCount == 1) investmentStatus = "투자 시작";
        else if (stockCount == 2) investmentStatus = "2개 종목 보유";
        else investmentStatus = "분산 투자 중";

        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyAvg", monthlyAvg);
        model.addAttribute("stockCount", stockCount);
        model.addAttribute("investmentStatus", investmentStatus);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("stockDetails", stockDetails);

        // ⭐ 목표 달성률 (중복 제거, 한 번만)
        Long goalTarget = (Long) session.getAttribute("goalTarget");
        int achieveRate = 0;
        long goalTargetWon = 0;
        if (goalTarget != null && goalTarget > 0) {
            goalTargetWon = goalTarget * 10000;
            achieveRate = (int) Math.min(100, monthlyAvg.longValue() * 100 / goalTargetWon);
        }
        model.addAttribute("achieveRate", achieveRate);
        model.addAttribute("goalTargetWon", goalTargetWon);

        return "dashboard";
    }
}