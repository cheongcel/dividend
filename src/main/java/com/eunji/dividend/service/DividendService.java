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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DividendService {

    private final DividendRepository dividendRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    private final Map<String, CachedStock> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000;

    @Transactional  // ⭐ 트랜잭션: 저장하다 에러나면 롤백! (안전장치)
    public Map<String, Object> calculateDividend(String ticker, int quantity) {
        // 티커 정리
        if (ticker.matches("[0-9]+") || (ticker.length() == 6 && !Character.isAlphabetic(ticker.charAt(0)))) {
            if (!ticker.endsWith(".KS")) ticker = ticker + ".KS";
        }
        String searchTicker = ticker.trim().toUpperCase();

        try {
            Stock stock = getStockWithCache(searchTicker);

            if (stock == null || stock.getQuote().getPrice() == null) {
                System.out.println("⚠️ API 실패 -> 플랜 B 가동: " + searchTicker);
                return getFallbackData(searchTicker, quantity);
            }

            String companyName = stock.getName();
            BigDecimal price = stock.getQuote().getPrice();
            BigDecimal dividendYield = stock.getDividend().getAnnualYieldPercent();
            if (dividendYield == null) dividendYield = BigDecimal.ZERO;

            // 주당 연 배당금
            BigDecimal annualDividendPerShare = price.multiply(dividendYield).divide(new BigDecimal(100));

            // DB 저장 및 업데이트
            saveToDb(searchTicker, companyName, price, annualDividendPerShare, quantity);

            return buildResult(companyName, price, annualDividendPerShare, quantity, searchTicker);

        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackData(searchTicker, quantity);
        }
    }

    private Stock getStockWithCache(String ticker) {
        CachedStock cached = cache.get(ticker);

        if (cached != null && !cached.isExpired()) {
            System.out.println("📦 캐시 사용: " + ticker);
            return cached.stock;
        }

        try {
            System.out.println("🌐 야후 API 호출: " + ticker);
            Stock stock = YahooFinance.get(ticker);
            if (stock != null) {
                cache.put(ticker, new CachedStock(stock));
            }
            return stock;
        } catch (Exception e) {
            System.out.println("🚫 API 호출 실패: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> getFallbackData(String ticker, int quantity) {
        String name = ticker + " (Simulated)";
        int price = 50000;
        int dividendPerShare = 1000;

        if (ticker.contains("005930") || ticker.contains("삼성전자")) {
            name = "Samsung Electronics";
            price = 74200;
            dividendPerShare = 1444;
        } else if (ticker.contains("AAPL")) {
            name = "Apple Inc.";
            price = 245000;
            dividendPerShare = 1350;
        } else if (ticker.contains("O")) {
            name = "Realty Income";
            price = 72000;
            dividendPerShare = 4200;
        }

        saveToDb(ticker, name, new BigDecimal(price), new BigDecimal(dividendPerShare), quantity);
        return buildResult(name, new BigDecimal(price), new BigDecimal(dividendPerShare), quantity, ticker);
    }

    private Map<String, Object> buildResult(String name, BigDecimal price, BigDecimal annualDivPerShare, int quantity, String ticker) {
        Map<String, Object> result = new HashMap<>();

        // 총 연 배당금 계산
        int totalAnnualDividend = annualDivPerShare.multiply(new BigDecimal(quantity)).intValue();

        // 월 배당금 계산
        int monthlyDividend = totalAnnualDividend / 12;

        result.put("ticker", ticker);
        result.put("companyName", name);
        result.put("price", price.intValue());
        result.put("dividendAmount", monthlyDividend);  // 화면엔 '월 배당금'으로 표시

        // ⭐ 중요: HTML 변수명과 일치시킴 (annualDividend -> totalAnnualDividend)
        result.put("totalAnnualDividend", totalAnnualDividend);

        // 월별 데이터 생성
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
            List<Integer> months = guessDividendMonths(ticker);
            String monthsStr = months.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            DividendEntity entity = dividendRepository.findByTicker(ticker);

            if (entity == null) {
                entity = new DividendEntity(
                        name,
                        ticker,
                        price.toString(),
                        annualDiv.toString(),
                        monthsStr
                );
            } else {
                // ⭐ 기존 데이터 업데이트 로직 (아주 훌륭함!)
                entity.setCompanyName(name);
                entity.setPrice(price.toString());
                entity.setDividend(annualDiv.toString());
                entity.setDividendMonths(monthsStr);
            }
            dividendRepository.save(entity);

            UserPortfolio myStock = userPortfolioRepository.findByTicker(ticker);
            if (myStock != null) {
                myStock.addQuantity(quantity);
            } else {
                myStock = new UserPortfolio(ticker, quantity);
            }
            userPortfolioRepository.save(myStock);

        } catch (Exception e) {
            System.out.println("⚠️ DB 저장 실패: " + e.getMessage());
        }
    }

    private List<Integer> guessDividendMonths(String ticker) {
        ticker = ticker.toUpperCase();
        if (ticker.contains("O") && ticker.length() < 3) {
            return Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12);
        } else if (ticker.endsWith(".KS")) {
            return Arrays.asList(4, 5, 8, 11);
        } else {
            return Arrays.asList(2, 5, 8, 11);
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