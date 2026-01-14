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

    // 3. 삭제하기 (새로 추가된 기능!)
    @PostMapping("/delete")
    public String deleteStock(@RequestParam("ticker") String ticker) {
        UserPortfolio portfolio = userPortfolioRepository.findByTicker(ticker);
        if (portfolio != null) {
            userPortfolioRepository.delete(portfolio);
        }
        return "redirect:/calendar";
    }

    // ⭐ [여기가 문제 해결의 핵심!] 캘린더 보여주기
    @GetMapping("/calendar")
    public String showCalendar(Model model) {
        // 1. 빈 달력 틀 만들기 (1월~12월)
        List<BigDecimal> monthlyTotals = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
        List<List<String>> monthlyDetails = new ArrayList<>();
        for (int i = 0; i < 12; i++) monthlyDetails.add(new ArrayList<>());

        // 2. 내 주식 가져오기
        List<UserPortfolio> myStocks = userPortfolioRepository.findAll();
        BigDecimal totalAnnual = BigDecimal.ZERO;

        // 숫자 예쁘게 꾸미기용 (3자리 콤마)
        DecimalFormat df = new DecimalFormat("#,###");

        for (UserPortfolio stock : myStocks) {
            DividendEntity info = dividendRepository.findByTicker(stock.getTicker());

            // 데이터가 없거나 이상하면 건너뛰기 (null 방지)
            if (info == null || info.getDividend() == null || info.getDividend().equals("null")) continue;

            try {
                // DB에서 "14500" 같은 글자를 숫자로 변환
                BigDecimal annualPerShare = new BigDecimal(info.getDividend());
                BigDecimal quantity = new BigDecimal(stock.getQuantity());

                // 내 총 배당금 = 주당배당금 * 수량
                BigDecimal myTotalAnnual = annualPerShare.multiply(quantity);
                totalAnnual = totalAnnual.add(myTotalAnnual);

                // 배당 월 분석 (예: "2,5,8,11")
                String monthsStr = info.getDividendMonths();
                String[] months;
                if (monthsStr == null || monthsStr.isEmpty()) {
                    months = new String[]{}; // 배당월 없으면 패스
                } else {
                    months = monthsStr.split(",");
                }

                // 월별로 나누기
                if (months.length > 0) {
                    // 소수점 버리고 계산 (나누기)
                    BigDecimal splitAmount = myTotalAnnual.divide(new BigDecimal(months.length), 0, RoundingMode.HALF_UP);

                    for (String m : months) {
                        try {
                            int monthIdx = Integer.parseInt(m.trim()) - 1; // 1월 -> 0번 인덱스
                            if (monthIdx >= 0 && monthIdx < 12) {
                                // 1. 총합 더하기
                                monthlyTotals.set(monthIdx, monthlyTotals.get(monthIdx).add(splitAmount));

                                // 2. 상세 내역 추가 ("삼성전자: 15,000원")
                                String detailText = info.getCompanyName() + ": " + df.format(splitAmount) + "원";
                                monthlyDetails.get(monthIdx).add(detailText);
                            }
                        } catch (Exception e) {
                            // 날짜 파싱 에러나면 무시
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ 캘린더 계산 중 에러 (" + stock.getTicker() + "): " + e.getMessage());
            }
        }

        // HTML로 데이터 보내기
        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("monthlyDetails", monthlyDetails); // 여기에 상세 내역이 들어있음!

        return "calendar";
    }

    @GetMapping("/goal")
    public String showGoal() { return "goal"; }
}