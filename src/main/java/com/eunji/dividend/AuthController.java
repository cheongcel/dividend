package com.eunji.dividend;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    // ========== 회원가입 페이지 ==========
    @GetMapping("/signup")
    public String showSignup() {
        return "signup";
    }

    // ========== 회원가입 처리 ==========
    @PostMapping("/signup")
    public String signup(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String name,
            Model model) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "signup";
        }

        // 간단한 유효성 검사
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            model.addAttribute("error", "모든 항목을 입력해주세요.");
            return "signup";
        }

        if (password.length() < 4) {
            model.addAttribute("error", "비밀번호는 4자 이상이어야 합니다.");
            return "signup";
        }

        // 사용자 생성 (⚠️ 비밀번호 평문 저장 - 나중에 암호화 필요)
        User user = new User(email, password, name);
        userRepository.save(user);

        // 회원가입 성공 → 로그인 페이지로
        return "redirect:/dashboard"; // ⭐ 인덱스 대신 대시보드로
    }

    // ========== 로그인 페이지 ==========
    @GetMapping("/login")
    public String showLogin(
            @RequestParam(required = false) String signup,
            @RequestParam(required = false) String redirectUrl,  // ⭐ 추가
            HttpSession session,
            Model model) {

        if ("success".equals(signup)) {
            model.addAttribute("message", "회원가입 성공! 로그인해주세요.");
        }
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            session.setAttribute("redirectAfterLogin", redirectUrl);
        }

        return "login";
    }

    // ========== 로그인 처리 ==========
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "존재하지 않는 이메일입니다.");
            return "login";
        }

        User user = userOpt.get();

        // 비밀번호 확인 (⚠️ 평문 비교 - 나중에 암호화 비교 필요)
        if (!user.getPassword().equals(password)) {
            model.addAttribute("error", "비밀번호가 틀렸습니다.");
            return "login";
        }

        // 로그인 성공 → 세션에 저장
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());

        String redirectUrl = (String) session.getAttribute("redirectAfterLogin");

        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            session.removeAttribute("redirectAfterLogin"); // 사용 후 삭제
            return "redirect:" + redirectUrl;
        }

        // 메인 페이지로
        return "redirect:/";
    }

    // ========== 로그아웃 ==========
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 삭제
        return "redirect:/"; // ⭐ 인덱스로 변경
    }
}