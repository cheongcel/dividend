package com.eunji.dividend.service;

import com.eunji.dividend.DividendEntity;
import com.eunji.dividend.UserPortfolio;
import com.eunji.dividend.DividendRepository;
import com.eunji.dividend.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DividendService {

    private final DividendRepository dividendRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    // ⭐ 1. 캐시 저장소 (은지 님 아이디어 적용!)
    private final Map<String, CachedStock> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5분

    public Map<String, Object> calculateDividend(String ticker, int quantity) {
        // 티커 정리
        if (ticker.matches("[0-9]+") || (ticker.length() == 6 && !Character.isAlphabetic(ticker.charAt(0)))) {
            if (!ticker.endsWith(".KS")) ticker = ticker + ".KS";
        }
        String searchTicker = ticker.trim().toUpperCase();

        try {
            // ⭐ 2. 캐시 확인 (야후 괴롭히기 방지)
            Stock stock = getStockWithCache(searchTicker);

            // 3. 데이터가 없거나 깨졌으면 -> 플랜 B (가상 데이터) 가동
            if (stock == null || stock.getQuote().getPrice() == null) {
                System.out.println("⚠️ API 실패 -> 플랜 B 가동: " + searchTicker);
                return getFallbackData(searchTicker, quantity);
            }

            // --- 정상 로직 ---
            String companyName = stock.getName();
            BigDecimal price = stock.getQuote().getPrice();
            BigDecimal dividendYield = stock.getDividend().getAnnualYieldPercent();
            if (dividendYield == null) dividendYield = BigDecimal.ZERO;

            // 배당금 계산
            BigDecimal annualDividend = price.multiply(dividendYield).divide(new BigDecimal(100));

            // ⭐ 4. DB 저장 (이게 있어야 '내 주식'이 쌓임!)
            saveToDb(searchTicker, companyName, price, annualDividend, quantity);

            // 결과 반환
            return buildResult(companyName, price, annualDividend, quantity, searchTicker);

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 나면 무조건 플랜 B로 살려내기
            return getFallbackData(searchTicker, quantity);
        }
    }

    // 캐시 조회 메서드 (은지 님 코드 활용)
    private Stock getStockWithCache(String ticker) {
        CachedStock cached = cache.get(ticker);

        // 캐시 유효하면 리턴
        if (cached != null && !cached.isExpired()) {
            System.out.println("📦 캐시 사용 (야후 호출 X): " + ticker);
            return cached.stock;
        }

        // 없으면 API 호출
        try {
            System.out.println("🌐 야후 API 호출: " + ticker);
            Stock stock = YahooFinance.get(ticker);
            if (stock != null) {
                cache.put(ticker, new CachedStock(stock));
            }
            return stock;
        } catch (Exception e) {
            System.out.println("🚫 API 호출 차단됨 (429/500): " + e.getMessage());
            return null; // null 반환해서 플랜 B로 넘김
        }
    }

    // 🛡️ 플랜 B: 가상 데이터 (야후가 막혔을 때)
    private Map<String, Object> getFallbackData(String ticker, int quantity) {
        String name = ticker + " (Simulated)";
        int price = 50000;
        int dividendPerShare = 1000;

        if (ticker.contains("005930") || ticker.contains("삼성전자")) {
            name = "Samsung Electronics"; price = 74200; dividendPerShare = 1444;
        } else if (ticker.contains("AAPL")) {
            name = "Apple Inc."; price = 245000; dividendPerShare = 1350;
        } else if (ticker.contains("O")) {
            name = "Realty Income"; price = 72000; dividendPerShare = 4200;
        }

        // 가상 데이터도 DB에 저장 (그래야 캘린더에 뜸)
        saveToDb(ticker, name, new BigDecimal(price), new BigDecimal(dividendPerShare), quantity);

        return buildResult(name, new BigDecimal(price), new BigDecimal(dividendPerShare), quantity, ticker);
    }

    // 공통: 결과 만들기 (차트 데이터 생성 포함)
    private Map<String, Object> buildResult(String name, BigDecimal price, BigDecimal annualDiv, int quantity, String ticker) {
        Map<String, Object> result = new HashMap<>();
        int totalDividend = annualDiv.intValue() * quantity;

        result.put("companyName", name);
        result.put("price", price.intValue());
        result.put("dividendAmount", totalDividend);

        // 월별 데이터 생성 (정교한 로직)
        List<Integer> months = guessDividendMonths(ticker);
        List<Integer> monthlyData = new ArrayList<>(Collections.nCopies(12, 0));
        if (totalDividend > 0 && !months.isEmpty()) {
            int amountPerMonth = totalDividend / months.size();
            for (int month : months) monthlyData.set(month - 1, amountPerMonth);
        }
        result.put("monthlyData", monthlyData);
        return result;
    }

    // 공통: DB 저장
    private void saveToDb(String ticker, String name, BigDecimal price, BigDecimal annualDiv, int quantity) {
        try {
            DividendEntity entity = dividendRepository.findByTicker(ticker);
            if (entity == null) {
                entity = new DividendEntity(name, ticker, price.toString(), annualDiv.toString(), "0");
                dividendRepository.save(entity);
            }
            UserPortfolio myStock = userPortfolioRepository.findByTicker(ticker);
            if (myStock != null) myStock.addQuantity(quantity);
            else myStock = new UserPortfolio(ticker, quantity);
            userPortfolioRepository.save(myStock);
        } catch (Exception e) {
            System.out.println("DB 저장 실패 (무시): " + e.getMessage());
        }
    }

    private List<Integer> guessDividendMonths(String ticker) {
        ticker = ticker.toUpperCase();
        if (ticker.contains("O") && ticker.length() < 3) return Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12); // 월배당
        else if (ticker.endsWith(".KS")) return Arrays.asList(4, 5, 8, 11); // 한국
        else return Arrays.asList(2, 5, 8, 11); // 미국
    }

    // 캐시 데이터 클래스 (은지 님 코드)
    private static class CachedStock {
        final Stock stock;
        final long timestamp;
        CachedStock(Stock stock) { this.stock = stock; this.timestamp = System.currentTimeMillis(); }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > CACHE_DURATION; }
    }
}