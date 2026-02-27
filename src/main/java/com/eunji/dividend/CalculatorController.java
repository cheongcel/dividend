package com.eunji.dividend;

import com.eunji.dividend.service.DividendService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // ⭐ API 키 주입
    @Value("${api.key:DEMO_KEY}")
    private String apiKey;

    @GetMapping("/")
    public String showMain() {
        return "index";
    }

    @GetMapping("/calculator")
    public String showCalculator(Model model) {
        // ⭐ API 키 전달
        model.addAttribute("apiKey", apiKey);
        return "calculator";
    }

    // 1. 계산만 하기 (저장 X) - 로그인 불필요
    @PostMapping("/calculate")
    public String calculate(@RequestParam("ticker") String ticker,
                            @RequestParam("quantity") int quantity,
                            Model model) {
        Map<String, Object> result = dividendService.calculateDividend(ticker, quantity, false, null);

        if (result.containsKey("error")) {
            model.addAttribute("error", result.get("error"));
        } else {
            model.addAttribute("result", result);
            model.addAttribute("searchTicker", ticker);
            model.addAttribute("searchQuantity", quantity);
        }

        // ⭐ API 키 전달
        model.addAttribute("apiKey", apiKey);
        return "calculator";
    }

    // 2. 추가하기 (저장 O) - ⭐ 로그인 필수!
    @PostMapping("/add")
    public String addStock(@RequestParam("ticker") String ticker,
                           @RequestParam("quantity") int quantity,
                           HttpSession session,
                           Model model) {
        // ⭐ 로그인 체크
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            // ⭐ 저장할 데이터를 세션에 임시 저장
            session.setAttribute("pendingTicker", ticker);
            session.setAttribute("pendingQuantity", quantity);

            // ⭐ 로그인 후 돌아올 URL 지정
            return "redirect:/login?redirectUrl=/add-pending";
        }

        // ⭐ 반환값 받기 (에러 체크)
        Map<String, Object> result = dividendService.calculateDividend(ticker, quantity, true, userId);

        if (result.containsKey("error")) {
            model.addAttribute("error", result.get("error"));
            model.addAttribute("apiKey", apiKey);
            return "calculator"; // 에러 시 다시 계산기로
        }

        return "redirect:/calendar";
    }

    // ⭐ 새로 추가: 로그인 후 저장 처리
    @GetMapping("/add-pending")
    public String addPendingStock(HttpSession session, Model model) {
        // 로그인 체크
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        // 임시 저장된 데이터 가져오기
        String ticker = (String) session.getAttribute("pendingTicker");
        Integer quantity = (Integer) session.getAttribute("pendingQuantity");

        if (ticker == null || quantity == null) {
            return "redirect:/calculator"; // 데이터 없으면 계산기로
        }

        // 세션에서 삭제
        session.removeAttribute("pendingTicker");
        session.removeAttribute("pendingQuantity");

        // 저장 처리
        Map<String, Object> result = dividendService.calculateDividend(ticker, quantity, true, userId);

        if (result.containsKey("error")) {
            model.addAttribute("error", result.get("error"));
            model.addAttribute("apiKey", apiKey);
            return "calculator";
        }

        return "redirect:/calendar";
    }

    // 3. 삭제하기 - ⭐ 로그인 필수!
    @PostMapping("/delete")
    public String deleteStock(@RequestParam("ticker") String ticker,
                              HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        UserPortfolio portfolio = userPortfolioRepository.findByUserIdAndTicker(userId, ticker);

        if (portfolio != null) {
            userPortfolioRepository.delete(portfolio);
        }
        return "redirect:/calendar";
    }

    // 4. 캘린더 보여주기 - ⭐ 누구나 볼 수 있음! (로그인 체크 제거)
    @GetMapping("/calendar")
    public String showCalendar(HttpSession session, Model model) {
        // ⭐ 로그인 체크 제거! (누구나 볼 수 있게)
        Long userId = (Long) session.getAttribute("userId");

        // 빈 달력 틀 만들기
        List<BigDecimal> monthlyTotals = new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO));
        List<List<String>> monthlyDetails = new ArrayList<>();
        for (int i = 0; i < 12; i++) monthlyDetails.add(new ArrayList<>());

        BigDecimal totalAnnual = BigDecimal.ZERO;
        List<UserPortfolio> myStocks = new ArrayList<>();

        // ⭐ 로그인 했을 때만 내 주식 조회
        if (userId != null) {
            myStocks = userPortfolioRepository.findByUserId(userId);

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
        }

        model.addAttribute("totalAnnual", totalAnnual);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("monthlyDetails", monthlyDetails);
        model.addAttribute("myStocks", myStocks);

        return "calendar";
    }

    // 5. 파이어족 목표 페이지 - ⭐ 누구나 볼 수 있음! (로그인 체크 제거)
    @GetMapping("/goal")
    public String showGoal(@RequestParam(value = "targetMonthly", required = false, defaultValue = "0") int targetMonthly,
                           HttpSession session,
                           Model model) {
        // ⭐ 로그인 체크 제거! (페이지는 누구나 볼 수 있게)
        Long userId = (Long) session.getAttribute("userId");

        List<UserPortfolio> myStocks = new ArrayList<>();
        BigDecimal currentAnnualDividend = BigDecimal.ZERO;

        // ⭐ 로그인 했을 때만 내 포트폴리오 조회
        if (userId != null) {
            myStocks = userPortfolioRepository.findByUserId(userId);

            for (UserPortfolio stock : myStocks) {
                DividendEntity info = dividendRepository.findByTicker(stock.getTicker());
                if (info != null && info.getDividend() != null && !info.getDividend().equals("null")) {
                    try {
                        BigDecimal stockTotal = new BigDecimal(info.getDividend()).multiply(new BigDecimal(stock.getQuantity()));
                        currentAnnualDividend = currentAnnualDividend.add(stockTotal);
                    } catch (Exception e) {}
                }
            }
        }

        model.addAttribute("targetMonthly", targetMonthly);
        model.addAttribute("currentAnnual", currentAnnualDividend);

        if (targetMonthly == 0) {
            model.addAttribute("progressPercent", 0);
            return "goal";
        }

        BigDecimal realTargetMonthly = new BigDecimal(targetMonthly).multiply(new BigDecimal("10000"));
        BigDecimal targetAnnual = realTargetMonthly.multiply(new BigDecimal(12));
        BigDecimal gap = targetAnnual.subtract(currentAnnualDividend);

        double percent = 0.0;
        if (targetAnnual.compareTo(BigDecimal.ZERO) > 0) {
            percent = currentAnnualDividend.doubleValue() / targetAnnual.doubleValue() * 100;
        }
        if (percent > 100) percent = 100;

        BigDecimal samsungDividend = new BigDecimal("1444");
        BigDecimal neededShares = BigDecimal.ZERO;
        BigDecimal gapAmount = BigDecimal.ZERO;

        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            gapAmount = gap;
            neededShares = gap.divide(samsungDividend, 0, RoundingMode.UP);
        }

        BigDecimal currentMonthly = currentAnnualDividend.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
        BigDecimal gapMonthly = BigDecimal.ZERO;
        if (gapAmount.compareTo(BigDecimal.ZERO) > 0) {
            gapMonthly = gapAmount.divide(new BigDecimal(12), 0, RoundingMode.HALF_UP);
        }

        model.addAttribute("progressPercent", String.format("%.1f", percent));
        model.addAttribute("neededShares", neededShares);
        model.addAttribute("gapAmount", gapAmount);
        model.addAttribute("currentMonthly", currentMonthly);
        model.addAttribute("gapMonthly", gapMonthly);

        return "goal";
    }
}