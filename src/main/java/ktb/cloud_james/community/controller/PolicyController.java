package ktb.cloud_james.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 정책 관련 페이지 컨트롤러
 * - 커뮤니티 이용약관, 개인정보처리방침 페이지 Thymeleaf로 서빙
 * - SSR 방식
 */
@Controller
@RequestMapping("/policy")
public class PolicyController {

    // 이용약관 페이지
    @GetMapping("/terms")
    public String terms() {
        return "policy/terms";
    }

    // 개인정보처리방침 페이지
    @GetMapping("/privacy")
    public String privacy() {
        return "policy/privacy";
    }
}
