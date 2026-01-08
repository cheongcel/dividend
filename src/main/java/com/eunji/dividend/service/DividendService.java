package com.eunji.dividend.service;

import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DividendService {

    // 계산기 기능 (주가 가져오기 + 배당금 계산 + 차트 데이터 생성)
    public Map<String, Object> calculateDividend(String ticker, int quantity) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 한국 주식 코드 변환 (숫자만 있으면 .KS 붙이기)
            if (ticker.matches("[0-9]+")) {
                ticker = ticker + ".KS";
            }

            // 1. 야후 파이낸스에서 정보 조회
            Stock stock = YahooFinance.get(ticker);

            if (stock == null || stock.getQuote().getPrice() == null) {
                result.put("error", "종목을 찾을 수 없어요 ㅠㅠ");
                return result;
            }

            // 2. 데이터 추출
            String companyName = stock.getName();
            BigDecimal price = stock.getQuote().getPrice();
            BigDecimal dividendYield = stock.getDividend().getAnnualYieldPercent();

            // 배당 수익률이 없으면 0으로 처리
            if (dividendYield == null) {
                dividendYield = BigDecimal.ZERO;
            }

            // 3. 배당금 계산
            // 연 배당금(주당) = 주가 * 수익률 / 100
            BigDecimal annualDividendPerShare = price.multiply(dividendYield).divide(new BigDecimal(100));
            int totalDividend = annualDividendPerShare.intValue() * quantity;
            int priceInt = price.intValue();

            // 4. 결과 담기
            result.put("companyName", companyName);
            result.put("price", priceInt);
            result.put("dividendAmount", totalDividend); // 총 배당금

            // ⭐ 5. 차트용 월별 데이터 만들기 (여기가 핵심!)
            List<Integer> months = guessDividendMonths(ticker);
            // 0이 12개 들어있는 리스트 생성 [0, 0, ... 0]
            List<Integer> monthlyData = new ArrayList<>(Collections.nCopies(12, 0));

            if (totalDividend > 0 && !months.isEmpty()) {
                int amountPerMonth = totalDividend / months.size(); // 월별 배당금
                for (int month : months) {
                    // month는 1월~12월, 리스트 인덱스는 0~11이라서 -1 해줌
                    monthlyData.set(month - 1, amountPerMonth);
                }
            }
            result.put("monthlyData", monthlyData); // 이 데이터를 HTML 차트가 씁니다!

        } catch (IOException e) {
            e.printStackTrace();
            result.put("error", "데이터를 가져오는 중 오류가 났어요.");
        }

        return result;
    }

    // 배당월 추측 (API가 날짜를 안 줘서 임시로 설정)
    private List<Integer> guessDividendMonths(String ticker) {
        ticker = ticker.toUpperCase();
        if (ticker.contains("O")) { // 리얼티인컴 (월배당)
            return Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        } else if (ticker.endsWith(".KS")) { // 한국 주식 (4,5,8,11월)
            return Arrays.asList(4, 5, 8, 11);
        } else { // 미국 주식 (2,5,8,11월)
            return Arrays.asList(2, 5, 8, 11);
        }
    }
}