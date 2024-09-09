package jpabook.jpashop.controller;

import jakarta.validation.Valid;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // get : 폼화면 열 때
    @GetMapping("/members/new")
    public String createForm(Model model){
        // Model : controller -> view로 넘어갈 때, 이 데이터를 실어서 넘김
        model.addAttribute("member", new Member());
        return "members/createMemberForm";

    }
    // post : 데이터를 받아서 등록
    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result){
        // - @Valid 어노테이션을 달면, MemberForm에 선언된 Validation체크를 사용하는구나 인식!
        //      - BindingResult : spring(validator)이 제공, validate 오류 발생 시, BindingReault에 오류가 담겨서 실행됨.(원래는 그냥 튕김)
        // - Member entity를 사용하지 않고, MemberForm을 사용하는 이유
        //      - 컨트롤러에서 원하는 validation과, 도메인이 원하는 validation이 달라질 수 있는데, 억지로 끼워맞추면서 코드가 지저분해진다
        //      - 진짜 단순할땐 써도되지만 실무에선 분리해서 쓰는게 심플함.
        //      - JPA 쓸때는 최대한 엔티티를 순수하게 유지하는 것이 좋음
        //      - dependency를 낮추고, 핵심 비즈니스 로직에만 dependency가 있도록 설계하는게 중요

        if(result.hasErrors()){
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);
        return "redirect:/"; // 저장 후 이동페이지
    }

    @GetMapping("/members")
    public String list(Model model){ // model이라는 객체를 통해서 화면에 데이터를 전달
        List<Member> members = memberService.findMembers();
        // 단순한 예제니까 그냥 엔티티를 반환하지만,
        // 실무에선 DTO로 변환해서 화면에서 필요한 데이터만 전달하도록 권장
        // 그리고, 지금은 화면을 그릴 때, 템플릿 엔진을 사용하여, 서버에서 렌더링이 되어서 필요한 데이터만 출력하지만,
        // * API를 만들땐 절-대 엔티티를 넘기면 안됨
        //    * api는 스펙임
        //    * 엔티티에 컬럼이 추가됐을 경우,
        //       - 문제1. 의도하지 않은 컬럼 노출. 문제2. api스펙이 변함
        //   => 엔티티에 로직이 추가되었는데, 그것으로 인해 API스펙이 변함 => 불안정한 API가 되버림

        model.addAttribute("members", members);
        // TIPS 리팩토링 단축키 shift ctrl alt T // 인라인으로 바꿔도됨
        return "members/memberList";
    }

}
