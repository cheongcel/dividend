package com.eunji.dividend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // @RestController에서 Rest를 뺐습니다. (화면을 보여주기 위해)
public class TestController {

    @GetMapping("/")
    public String main() {
        return "index"; // "index.html 파일을 보여줘라"라는 뜻
    }
}