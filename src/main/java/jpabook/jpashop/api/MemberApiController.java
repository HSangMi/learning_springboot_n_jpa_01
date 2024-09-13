package jpabook.jpashop.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController //@Controller@ResponseBody
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v1/members")
    public List<Member> membersV1(){
        return memberService.findMembers();
        /*
        엔티티를 직접 노출하게 되면, 엔티티 정보들이 모두 외부에 노출됨
        @JsonIgnore로 정보노출을 막을 순 있음
        but
         문제 1. 엔티티에 presentation 로직이 포함되버림!!! 나쁨!
         문제 2. 엔티티 컬럼명이 바뀌게 되면 api 스펙이바뀌어버림
         문제 3. 클라이언트마다 요구하는 형식이 다를 수 있음
         문제 4. 리스트형식을 직접 반환하면안됨!! 이후 추가하고싶은 정보가 있을 때, json 스펙이 깨져버림
            리스트를 반환할경우 : [ {}, {}, {}, ...] // 추가적인 정보를 포함하기 어려움. 유연성x
            map으로 한번 묶어서 반환: {"data":[{}, {}, {}], 데이터 추가가능}
        */
    }
    @GetMapping("/api/v2/members")
    public Result membersV2() {
        List<Member> findMembers = memberService.findMembers();
        //List<Member> -> List<MemberDto>
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());
        return new Result(collect.size(), collect);
        // 한번 감싸줘야지, 안그러면 json 배열타입으로 반환되서 유연성이 떨어임
    }

    @Data
    @AllArgsConstructor
    static class Result<T>{
        private int count;
        private T data;
    }
    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String name;
    }
    // ver 1
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member){
        //@RequestBody : json 형식으로 넘어온 데이터를 Member타입 객체에 넣어줌

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
        /*
        문제점 1
            @Valid Member member
            - presentation 계층을 위한 검증로직(@Vaild, @NotEmpty)이 엔티티에 들어가있는게 문제!
            => api 종류에 따라, not empty 일수도, 아닐 수 도 있는데!!
        문제점 2
            - Entity의 컬럼명이 바뀔경우 (ex: name -> username)
            api 스펙이 바뀌어버림!!!
            => API 스펙을 위한 별도의 DTO를 만들어서 사용해야함. 엔티티를 파라미터로 받지말자!!
        */
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request){
        /*
        장점 1
            - DTO를 통해 파라미터를 전달받으므로, 엔티티가 바뀌어도 API 스펙이 변하진 않는다!
        장점 2
            - 엔티티를 넘길 경우, 어떤 값이 넘어올지 모르는데, DTO는 자체가 스펙이기 때문에 어떤값이 넘어올 지 알수있다.
         */
        Member member = new Member();
        member.setName(request.getName());
        Long id= memberService.join(member);
        return new CreateMemberResponse(id);
    }

    // 회원정보 수정하기 RestAPI 스타일
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id
            , @RequestBody @Valid UpdateMemberRequest request){
        // 등록이랑 수정은 api스펙이 거의 다 다름. 수정은 굉장히 제한적 -> DTO를 따로두는편을 권장

        // 수정시엔 되도록 >>변경감지<<를 쓸 것!
        memberService.update(id, request.getName());
        /*
           update 후 Member를 반환 할 수도있지만,(영속성이 끊긴 객체가 반환됨 ㄱㅊ)
           커맨드(명령-변경성 메소드), 쿼리(조회)를 철처히 분리 것을 선호(취향)
           member를 반환하게 되면, 커맨드&쿼리하는 형태가 됨
           => 변경된 entity를 반환하기 보단, id값 정도까지 반환하는 형태를 선호
         */
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());

    }

    // 엔티티엔 lombok을 제한적으로 쓰지만(@Getter 정도,,) DTO엔 비교적 많이쓰는편
    @Data
    static class UpdateMemberRequest {
        private String name;
    }
    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }


    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
