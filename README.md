💰 DIVY - Dividend Calculator
"Visualize your dividend cash flow in just 3 seconds." An intuitive asset management service designed to eliminate the complexity of dividend tracking and portfolio planning.


📸 Screenshots

### Dashboard
![Dashboard](screenshots/dashboard.png)
Real-time portfolio analytics with Chart.js visualization

### Calculator
![Calculator](screenshots/calculator.png)
Instant dividend calculation for US stocks

### Calendar
![Calendar](screenshots/calendar.png)
12-month dividend distribution view

### Goal Tracker
![Goal](screenshots/goal.png)
Track your financial independence progress


🎯 Strategic Overview
Many dividend investors struggle with fragmented data across different exchanges and the tedious task of manual calculation in spreadsheets. DIVY provides a seamless, "zero-barrier" experience for data-driven investment.

The Problem: Fragmented dividend schedules (monthly/quarterly) and complex currency conversions lead to high cognitive load.

The Solution: An automated dashboard that fetches, calculates, and visualizes dividend data with a single ticker input, reducing tracking time by 90%.


✨ Key Features
Personalized Dashboard: Dynamic visualization of annual/monthly dividend trends using Chart.js.

Smart Calculator: Real-time calculation for KR/US stocks with automatic currency conversion logic.

Dynamic Calendar: 12-month distribution view with a toggle between Calendar and List formats for better cash flow planning.

Goal Tracker: Reverse-calculates the required capital (Required Asset) to reach specific monthly passive income goals.

🏗️ Tech Stack
Backend
Java 17 / Spring Boot 3.2

Spring Data JPA

Session-based Authentication (BCrypt encryption)

H2 (Dev) / PostgreSQL (Prod)

Frontend
Thymeleaf (Server-side Rendering)

Vanilla JavaScript & Chart.js

CSS3 (Mobile-first Responsive Design)

🎨 Engineering Challenges & Solutions
1. Financial Data Integrity (Precision Management)
Challenge: Floating-point errors during dividend distribution across multiple months can lead to inaccurate financial reporting. Solution: Utilized BigDecimal for all monetary calculations. Implemented a distribution algorithm using RoundingMode.HALF_UP to ensure "cent-perfect" accuracy.

// Even distribution logic for quarterly/annual dividends
BigDecimal splitAmount = totalDividend.divide(
    new BigDecimal(dividendMonths.length), 
    2, // Precision to 2 decimal places
    RoundingMode.HALF_UP
);

2. Strategic UX Flow (Conversion Optimization)
Challenge: High user drop-off rates due to mandatory sign-up requirements. Solution: Implemented a "Value-First" funnel. Users can access the calculator immediately. Core features (Save/Sync) are protected by a CSS blur effect and login redirection, successfully balancing feature preview and user acquisition.

3. RESTful API & Data Isolation
Challenge: Maintaining clean separation between user portfolios while providing a RESTful interface. Solution: Standardized API endpoints and enforced strict data isolation at the service layer using Spring Security sessions.

Method,Endpoint,Description
GET,/api/v1/dashboard,Fetch aggregated portfolio stats
POST,/api/v1/calculate,Mock-calculate dividend without saving
POST,/api/v1/portfolio,Add ticker to user's verified portfolio
DELETE,/api/v1/portfolio/{id},Remove specific stock from portfolio

📊 Database Schema
erDiagram
    USERS ||--o{ USER_PORTFOLIO : owns
    USER_PORTFOLIO }|--|| DIVIDEND_DATA : references
    
    USERS {
        long id PK
        string email UK
        string password
        timestamp created_at
    }
    USER_PORTFOLIO {
        long id PK
        long user_id FK
        string ticker
        int quantity
    }
    DIVIDEND_DATA {
        string ticker PK
        string company_name
        decimal dividend
        string dividend_months
        decimal price
    }

🚀 Getting Started
  1. Clone & Setup
    git clone https://github.com/cheongcel/dividend.git
    cd dividend

  2. Run with Maven
./mvnw spring-boot:run
Access the application at http://localhost:8080


📈 Future Roadmap
[ ] Real-time API: Transition from static data to Live Market Data API integration.

[ ] Test Coverage: Implementation of JUnit5/Mockito tests (Target: 80% coverage).

[ ] Security: Migration to JWT for stateless mobile-friendly authentication.

📝 License
MIT License

👤 Developer

Cheongcel
GitHub: @cheongcel
Email: andfrank@naver.com
