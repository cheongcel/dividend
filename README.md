💰 DIVY - Dividend Calculator
"Visualize your dividend cash flow in just 3 seconds." An intuitive asset management service designed to eliminate the complexity of dividend tracking and portfolio planning.

🎯 Strategic Overview
Many dividend investors struggle with fragmented data across different exchanges and the tedious task of manual calculation in spreadsheets. DIVY solves this by providing a seamless, "zero-barrier" experience.

The Problem: Complex manual calculations for monthly/quarterly dividend cycles and tedious currency conversions.

The Solution: An automated dashboard that fetches, calculates, and visualizes dividend data with a single ticker input.

📸 Screenshots
(-)

✨ Key Features
Personalized Dashboard: Real-time visualization of annual/monthly dividend trends using Chart.js.

Smart Calculator: Instant calculation for both KOSPI/KOSDAQ and US stocks with automatic currency conversion.

Dynamic Calendar: Monthly dividend distribution view with a toggle between Calendar and List formats.

Investment Goal Tracker: Reverse-calculates the required capital to reach specific monthly passive income goals.

🏗️ Tech Stack
Backend
Java 17 / Spring Boot 3.2

Spring Data JPA

Spring Security (Session-based Authentication)

H2 (Dev) / PostgreSQL (Prod)

Frontend
Thymeleaf (Server-side Rendering)

Vanilla JavaScript & Chart.js

CSS3 (Responsive Design & UI/UX)

🎨 Engineering Challenges & Solutions
1. Financial Data Integrity
Challenge: Precision issues when distributing annual dividends into monthly slots and handling exchange rates. Solution: Implemented BigDecimal for all monetary calculations to prevent floating-point errors. Used RoundingMode.HALF_UP to ensure cent-perfect accuracy during monthly distribution logic.

2. Optimizing User Conversion (UX)
Challenge: High bounce rates often occur when forced to sign up before experiencing the product's value. Solution: Designed a "Calculator-First" flow. Users can use the core calculator immediately; a CSS blur effect and seamless login redirection are used only when the user attempts to "Save" the data, successfully increasing the potential sign-up conversion.

3. RESTful API Design & Data Isolation
Challenge: Ensuring secure and intuitive data access for personalized portfolios. Solution: Structured the API following REST principles and utilized Spring Security sessions to ensure strict data isolation between users.

HTTP

GET    /api/v1/dashboard    - Fetch calculated portfolio statistics
POST   /api/v1/portfolios   - Add a new ticker to user's collection
DELETE /api/v1/portfolios   - Remove a stock from the portfolio
PATCH  /api/v1/goals        - Update investment target goals
🚀 Getting Started
(Keep your existing Getting Started section here)

📈 Future Roadmap
[ ] Real-time Integration: Moving from static data to Alpha Vantage/Yahoo Finance API.

[ ] Test Excellence: Increasing Unit Test coverage to 80% with JUnit5 & Mockito.

[ ] Push Notifications: Automated alerts for upcoming "Ex-Dividend" dates.

👤 Developer

Cheongcel
GitHub: @cheongcel
Email: andfrank@naver.com
