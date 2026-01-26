 DIVY - 배당금 계산기

>  직관적한 배당금 관리 서비스


 🎯 프로젝트 소개

DIVY는 배당 투자자를 위한 올인원 배당금 관리 플랫폼입니다.  
복잡한 계산 없이 3초 만에 내 배당금을 확인하고, 목표를 설정할 수 있습니다.

 📸 주요 화면

[대시보드](screenshots/dashboard.png)
실시간 배당금 현황을 한눈에

[계산기](screenshots/calculator.png)
간단한 입력으로 즉시 계산

[캘린더](screenshots/calendar.png)
월별 배당금을 달력으로 확인


✨ 주요 기능

 💼 개인화된 대시보드
- 연간/월간 예상 배당금 한눈에 확인
- Chart.js 기반 월별 배당금 추이 시각화
- 보유 종목별 배당률 분석

 🧮 실시간 배당금 계산기
- 종목 코드 입력만으로 즉시 계산
- 한국/미국 주식 지원
- 환율 자동 적용

 📅 월별 배당 캘린더
- 12개월 배당금 분포 확인
- 달력형/리스트형 뷰 전환
- 종목별 상세 내역

 🎯 경제적 자유 로드맵
- 목표 금액 설정
- 현재 달성률 시각화
- 필요한 투자금액 계산


 🏗️ 기술 스택

 Backend
- Java 17
- Spring Boot 3.2
- Spring Data JPA
- Session 기반 인증

 Frontend
- Thymeleaf
- Chart.js (데이터 시각화)
- Vanilla JavaScript
- CSS3

 Database
- PostgreSQL (Production)
- H2 (Development)

 Deploy
- Render.com


 🎨 기술적 특징

 1. UX/UI
- 직관적인 사용자 플로우
- 부드러운 애니메이션 (CSS transition)

 2. Chart.js 커스터마이징
- 월별 배당금 라인 차트
- 반응형 차트 (모바일 대응)
- 호버 시 상세 정보 표시

 3. 사용자별 데이터 분리
- 세션 기반 인증
- 개인화된 포트폴리오 관리
- 보안을 고려한 데이터 접근 제어

 4. RESTful API 설계
GET  /dashboard    - 대시보드
POST /calculate    - 배당금 계산
POST /add         - 포트폴리오 추가
POST /delete      - 종목 삭제
GET  /calendar    - 월별 캘린더
GET  /goal        - 목표 설정



 📊 데이터베이스 구조
```sql
users
- id (PK)
- email (UNIQUE)
- password (BCrypt 암호화)
- name
- created_at

user_portfolio
- id (PK)
- user_id (FK)
- ticker
- quantity

dividend_data
- ticker (PK)
- company_name
- dividend
- dividend_months
- price

 🚀 실행 방법

### 1. Clone
```bash
git clone https://github.com/yourusername/DIVY.git
cd DIVY
```

### 2. 환경변수 설정
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/divy
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. 실행
```bash
./mvnw spring-boot:run
```

### 4. 접속
```
http://localhost:8080
```

<br>

 🌐 배포

🔗 Live Demo:** https://divy.up.railway.app

- Railway를 통한 자동 배포
- PostgreSQL 데이터베이스 연동
- 환경변수 기반 설정 관리



 💡 개발 과정에서 고민한 점

 1. UX 설계
 문제: 배당금 계산이 복잡하고 진입장벽이 높음

해결:
- 토스 UX 벤치마킹 (3초 안에 핵심 기능 접근)
- 비로그인 시 블러 처리로 후킹
- 계산기 → 저장 → 로그인 → 캘린더 자연스러운 플로우

 2. 월별 배당금 분배 로직
 문제: 주식마다 배당 월이 다름 (분기별, 월별, 연간)

해결:
java
// 배당 월 수로 나눠서 분배
BigDecimal splitAmount = totalDividend.divide(
    new BigDecimal(dividendMonths.length), 
    RoundingMode.HALF_UP
);
```

 3. 차트 반응형 처리
 문제: Chart.js 기본 설정은 반응형이 완벽하지 않음

해결:
```javascript
options: {
    responsive: true,
    maintainAspectRatio: false
}
```

 📈 향후 개선 계획

- [ ] 실시간 주가 API 연동
- [ ] 배당 히스토리 추적
- [ ] 엑셀 다운로드 기능
- [ ] 알림 기능 (배당 지급일)
- [ ] 테스트 코드 작성 (목표: 80% 커버리지)


 📝 라이선스

MIT License


👤 개발자

Cheongcel
- GitHub: [@cheongcel](https://github.com/cheongcel/dividend)
- Email: andfrank@naver.com



⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!
