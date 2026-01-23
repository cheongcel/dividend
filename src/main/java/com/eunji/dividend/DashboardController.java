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

        // 비로그인 시 랜딩 페이지로
        if (userId == null) {
            return "redirect:/";
        }

        // 내 포트폴리오 가져오기
        List<UserPortfolio> myStocks = userPortfolioRepository.findByUserId(userId);

        // 월별 배당금 계산
        List<BigDecimal> monthlyTotals = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
        BigDecimal totalAnnual = BigDecimal.ZERO;
        BigDecimal totalAssetValue = BigDecimal.ZERO;

        DecimalFormat df = new DecimalFormat("#,###");

        // 주식별 데이터
        List<Map<String, Object>> stockDetails = new ArrayList<>();

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());

            if (info == null) continue;

            try {
                // 배당금 계산
                BigDecimal annualPerShare = info.getDividend() != null && !info.getDividend().equals("null")
                        ? new BigDecimal(info.getDividend())
                        : BigDecimal.ZERO;
                BigDecimal quantity = new BigDecimal(stock.getQuantity());
                BigDecimal myTotalAnnual = annualPerShare.multiply(quantity);
                totalAnnual = totalAnnual.add(myTotalAnnual);

                // 자산 가치
                BigDecimal price = info.getPrice() != null && !info.getPrice().equals("null")
                        ? new BigDecimal(info.getPrice())
                        : BigDecimal.ZERO;
                BigDecimal assetValue = price.multiply(quantity);
                totalAssetValue = totalAssetValue.add(assetValue);

                // 월별 분배
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

                // 주식 상세 정보
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

        // 월 평균
        BigDecimal monthlyAvg = totalAnnual.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);

        // 모델에 추가
        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyAvg", monthlyAvg);
        model.addAttribute("totalAssetValue", totalAssetValue);
        model.addAttribute("stockCount", myStocks.size());
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("stockDetails", stockDetails);

        return "dashboard";
    }
}