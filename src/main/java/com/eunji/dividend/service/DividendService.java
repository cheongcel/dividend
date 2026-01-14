package com.eunji.dividend.service;

import com.eunji.dividend.DividendEntity;
import com.eunji.dividend.UserPortfolio;
import com.eunji.dividend.DividendRepository;
import com.eunji.dividend.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DividendService {

    private final DividendRepository dividendRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final Map<String, CachedStock> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5분

    // 고정 환율 (나중에 실시간 API로 교체 가능)
    private static final BigDecimal USD_TO_KRW = new BigDecimal("1450");

    // 기본 호환성 유지
    public Map<String, Object> calculateDividend(String ticker, int quantity) {
        return calculateDividend(ticker, quantity, true);
    }

    @Transactional
    public Map<String, Object> calculateDividend(String ticker, int quantity, boolean saveMode) {
        // 한국 주식 티커 정리
        if (ticker.matches("[0-9]+") || (ticker.length() == 6 && !Character.isAlphabetic(ticker.charAt(0)))) {
            if (!ticker.endsWith(".KS")) ticker = ticker + ".KS";
        }
        String searchTicker = ticker.trim().toUpperCase();

        try {
            Stock stock = getStockWithCache(searchTicker);

            // API 실패하거나 주가 없으면 플랜 B
            if (stock == null || stock.getQuote() == null || stock.getQuote().getPrice() == null) {
                System.out.println("⚠️ API 실패 -> 플랜 B: " + searchTicker);
                return getFallbackData(searchTicker, quantity, saveMode);
            }

            String companyName = stock.getName();
            BigDecimal priceUSD = stock.getQuote().getPrice();
            BigDecimal dividendYield = stock.getDividend().getAnnualYieldPercent();

            if (dividendYield == null) dividendYield = BigDecimal.ZERO;

            // 환율 적용
            boolean isKorean = searchTicker.endsWith(".KS");
            BigDecimal priceKRW = isKorean ? priceUSD : priceUSD.multiply(USD_TO_KRW);

            // 주당 연 배당금 계산
            BigDecimal annualDividendPerShare = priceKRW
                    .multiply(dividendYield)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

            // 저장 모드일 때만 DB 저장
            if (saveMode) {
                saveToDb(searchTicker, companyName, priceKRW, annualDividendPerShare, quantity);
            }

            return buildResult(companyName, priceKRW, annualDividendPerShare, quantity, searchTicker);

        } catch (Exception e) {
            System.out.println("⚠️ 에러 발생 -> 플랜 B 가동: " + e.getMessage());
            e.printStackTrace();
            return getFallbackData(searchTicker, quantity, saveMode);
        }
    }

    private Map<String, Object> getFallbackData(String ticker, int quantity, boolean saveMode) {
        String name = ticker + " (Simulated)";
        BigDecimal price = new BigDecimal("50000");
        BigDecimal dividendPerShare = new BigDecimal("1000");


        // 미국 주식이면 환율 적용
        if (!ticker.endsWith(".KS")) {
            if (ticker.contains("AAPL")) {
                name = "Apple Inc.";
                price = new BigDecimal("250").multiply(USD_TO_KRW);
                dividendPerShare = new BigDecimal("1.0").multiply(USD_TO_KRW);
            } else if (ticker.contains("O")) {
                name = "Realty Income";
                price = new BigDecimal("55").multiply(USD_TO_KRW);
                dividendPerShare = new BigDecimal("3.0").multiply(USD_TO_KRW);
            }
        } else if (ticker.contains("005930")) {
            name = "Samsung Electronics";
            price = new BigDecimal("74200");
            dividendPerShare = new BigDecimal("1444");
        }

        if (saveMode) {
            saveToDb(ticker, name, price, dividendPerShare, quantity);
        }

        return buildResult(name, price, dividendPerShare, quantity, ticker);
    }

    private Map<String, Object> buildResult(String name, BigDecimal price, BigDecimal annualDivPerShare, int quantity, String ticker) {
        Map<String, Object> result = new HashMap<>();

        // 총 연 배당금
        int totalAnnualDividend = annualDivPerShare
                .multiply(new BigDecimal(quantity))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        // 월 배당금
        int monthlyDividend = totalAnnualDividend / 12;

        result.put("ticker", ticker);
        result.put("companyName", name);
        result.put("price", price.setScale(0, RoundingMode.HALF_UP).intValue());
        result.put("dividendAmount", monthlyDividend);
        result.put("totalAnnualDividend", totalAnnualDividend);

        // 월별 배당 데이터
        List<Integer> months = guessDividendMonths(ticker);
        List<Integer> monthlyData = new ArrayList<>(Collections.nCopies(12, 0));

        if (totalAnnualDividend > 0 && !months.isEmpty()) {
            int amountPerPayment = totalAnnualDividend / months.size();
            for (int month : months) {
                monthlyData.set(month - 1, amountPerPayment);
            }
        }

        result.put("monthlyData", monthlyData);
        return result;
    }

    private void saveToDb(String ticker, String name, BigDecimal price, BigDecimal annualDiv, int quantity) {
        try {
            // 배당월 문자열 생성
            List<Integer> months = guessDividendMonths(ticker);
            String monthsStr = months.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // DividendEntity 저장/업데이트
            DividendEntity entity = dividendRepository.findByTicker(ticker);

            if (entity == null) {
                entity = new DividendEntity(
                        name,
                        ticker,
                        price.setScale(0, RoundingMode.HALF_UP).toString(),
                        annualDiv.setScale(0, RoundingMode.HALF_UP).toString(),
                        monthsStr
                );
            } else {
                entity.setCompanyName(name);
                entity.setPrice(price.setScale(0, RoundingMode.HALF_UP).toString());
                entity.setDividend(annualDiv.setScale(0, RoundingMode.HALF_UP).toString());
                entity.setDividendMonths(monthsStr);
            }
            dividendRepository.save(entity);

            // UserPortfolio 저장/업데이트
            UserPortfolio myStock = userPortfolioRepository.findByTicker(ticker);
            if (myStock != null) {
                myStock.addQuantity(quantity);
            } else {
                myStock = new UserPortfolio(ticker, quantity);
            }
            userPortfolioRepository.save(myStock);

            System.out.println("✅ DB 저장 완료: " + ticker);

        } catch (Exception e) {
            System.out.println("⚠️ DB 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ⭐ 이 부분이 핵심! 캐시에 저장해야 함!
    private Stock getStockWithCache(String ticker) {
        CachedStock cached = cache.get(ticker);

        // 캐시가 있고 유효하면 재사용
        if (cached != null && !cached.isExpired()) {
            System.out.println("📦 캐시 사용 (API 호출 안 함): " + ticker);
            return cached.stock;
        }

        // 캐시가 없거나 만료됐으면 API 호출
        try {
            System.out.println("🌐 야후 API 호출: " + ticker);
            Stock stock = YahooFinance.get(ticker);

            // ⭐ 성공하면 캐시에 저장!
            if (stock != null) {
                cache.put(ticker, new CachedStock(stock));
                System.out.println("💾 캐시 저장 완료: " + ticker);
            }

            return stock;
        } catch (Exception e) {
            System.out.println("🚫 야후 API 실패: " + e.getMessage());
            return null;
        }
    }

    private List<Integer> guessDividendMonths(String ticker) {
        ticker = ticker.toUpperCase();
        if (ticker.contains("O") && ticker.length() < 3) {
            return Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12); // 월배당
        } else if (ticker.endsWith(".KS")) {
            return Arrays.asList(4, 5, 8, 11); // 한국 주식
        } else {
            return Arrays.asList(2, 5, 8, 11); // 미국 주식
        }
    }

    private static class CachedStock {
        final Stock stock;
        final long timestamp;

        CachedStock(Stock stock) {
            this.stock = stock;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
}