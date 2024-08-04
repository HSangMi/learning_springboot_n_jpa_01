package jpabook.jpashop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model){
        model.addAttribute("data","hello!");
        // 스프링부트 + thymeleaf viewName 맵핑 기본값
        // resources:templates + "화면이름" +.html 화면으로 이동
        return "hello";
    }
}
