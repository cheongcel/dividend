package com.eunji.dividend;

// 이 클래스는 '주식 하나'의 정보를 담는 그릇입니다.
public class Dividend {

    private String ticker;    // 종목 코드 (예: AAPL, O)
    private double price;     // 배당금 (예: 0.24 달러)
    private String cycle;     // 배당 주기 (Monthly, Quarterly)

    // 생성자 (데이터 채워넣기용)
    public Dividend(String ticker, double price, String cycle) {
        this.ticker = ticker;
        this.price = price;
        this.cycle = cycle;
    }

    // 데이터를 꺼내볼 수 있게 하는 getter (단축키 Alt+Insert로 만들 수도 있음)
    public String getTicker() { return ticker; }
    public double getPrice() { return price; }
    public String getCycle() { return cycle; }
}