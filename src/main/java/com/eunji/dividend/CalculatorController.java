package com.eunji.dividend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================================
    // 🔑복사한 FMP API 키
    // ========================================================
    private final String API_KEY = "kuW27XRpN6heNXulR7gwOQyaN2cPULSY";
    // 예: private final String API_KEY = "1a2b3c4d5e...";

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
            // 1. [캐시 확인] DB에 있으면 DB 씀 (아까운 무료 횟수 아끼기 위해)
            DividendEntity existingData = dividendRepository.findByTicker(ticker);

            if (existingData != null) {
                System.out.println("⚡ [캐시 적중] DB 사용: " + ticker);
                name = existingData.getCompanyName();
                price = new BigDecimal(existingData.getPrice());
                monthlyDividend = new BigDecimal(existingData.getDividend());
            } else {
                System.out.println("🚀 [FMP API 요청] 진짜 데이터 가지러 감: " + ticker);

                // 2. FMP API 주소 만들기
                // 한국 주식(.KS)은 FMP에서 인식이 잘 안될 수 있어서 일단 미국 주식 위주로 테스트 추천
                String url = "https://financialmodelingprep.com/api/v3/profile/" + ticker + "?apikey=" + API_KEY;

                // 3. 데이터 가져오기 (JSON)
                String jsonResult = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .timeout(10000)
                        .execute()
                        .body();

                // 4. JSON 해석
                JsonNode root = objectMapper.readTree(jsonResult);

                // 데이터가 비어있으면 (없는 종목)
                if (root.isEmpty()) {
                    throw new RuntimeException("FMP에서 종목을 찾을 수 없습니다. (" + ticker + ")");
                }

                JsonNode data = root.get(0); // 첫 번째 결과

                // 회사 이름
                name = data.path("companyName").asText();

                // 가격
                double rawPrice = data.path("price").asDouble();
                price = new BigDecimal(String.valueOf(rawPrice));

                // 마지막 배당금 (lastDiv) - FMP는 '최근 지급된 배당금'을 줍니다.
                double lastDiv = data.path("lastDiv").asDouble();

                // 환율 적용 (한국 주식 아니면)
                if (!ticker.endsWith(".KS")) {
                    BigDecimal exchangeRate = new BigDecimal("1430");
                    price = price.multiply(exchangeRate);
                    // lastDiv는 보통 연간 배당 기준이거나 최근 배당일 수 있음. MVP에선 연배당으로 가정
                    BigDecimal annualDividend = new BigDecimal(String.valueOf(lastDiv)).multiply(exchangeRate);

                    // 월 배당금 계산
                    monthlyDividend = annualDividend
                            .multiply(new BigDecimal(quantity))
                            .divide(new BigDecimal(12), 0, BigDecimal.ROUND_HALF_UP);
                } else {
                    // 한국 주식인 경우
                    monthlyDividend = new BigDecimal(String.valueOf(lastDiv))
                            .multiply(new BigDecimal(quantity))
                            .divide(new BigDecimal(12), 0, BigDecimal.ROUND_HALF_UP);
                }

                // 5. DB 저장
                DividendEntity entity = new DividendEntity(
                        name, ticker, price.toString(), monthlyDividend.toString()
                );
                dividendRepository.save(entity);
                System.out.println("✅ [FMP] 정식 데이터 저장 완료: " + name);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ FMP 에러: " + e.getMessage());
            model.addAttribute("error", "API 오류: " + e.getMessage() + " (키를 확인해주세요)");
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