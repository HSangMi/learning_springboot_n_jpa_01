package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/**
 * jpa에서 모든 데이터 변경이나, 로직들은 가급적 트랜잭션 안에서 실행되어야 함.
     - class 레벨에서 설정해 두면, 하위 public 메소들에 대해 모두 적용됨
     - Transactional 어노테이션이 두개 있는데, spring에서 제공하는 어노테이션, javax에서 제공하는 어노테이션
     - 이미 spring에 denpency하기 떄문에 spring꺼 쓰는게 나음 -> 더 많은 spring에서 제공하는 편의 옵션 사용가능

*  readOnly옵션을 true로하면 jpa가 조회하는 쪽에서 성능을 최적화 함
    - 영속성 컨택스트를 flush를 안하거나, 더티체킹을 안하고,, 등의 이점
    - database에 따라서 읽기전용 트랜잭션이니까 db에 단순 읽기전용이니 많은 리소스를 사용할 필요 없도록 알려주는 드라이버 들도 있음
    - defualt 설정은 readOnly=false
    - class레벨 설정을 해당 repository의 성격에 따라 true/false를 설정하고, 예외적인 메소드에만 따로지정
 */
@Transactional(readOnly = true)
@RequiredArgsConstructor // final 키워드가 있는 필드만 가지고 생성자를 만들어줌
public class MemberService {
/*
//  field injection => 변경을 못하는 단점
    @Autowired


    // setter injection
    // 장점 : 테스트 코드 작성 시 mock 객체로 주입가능
    // 단점 : 바뀔 위험이 있음. 애플리케이션을 로딩하는 시점에, 세팅 조립이 다 끝나고 바꿀일은 사실 거의 없음
    @Autowired
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 생성자 injection : best!
    // 테스트 케이스 작성 시, 직접 주입도 가능하고,
    // 생성자에 기입함으로 써, 의존관계를 명확히 확인 할 수 있음
    // 처음 구동시에 셋팅되어 바뀔일도 없음.
    @Autowired  // 생성자가 하나밖에 없을 경우, @Autowired 생략 가능 (스프링이 알아서 주입해줌)
   // Lombok의 @AllArgsConstructor로 대체 가능 : 모든필드로 생성자 만들어줌
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    // injection 최종 진화형
    // @RequiredArgsConstructor : final 키워드가 있는 필드만 가지고 생성자를 만들어줌
    */

    // final 키워드를 사용하면, 초기화가 안되어있는 경우 컴파일에러로 알려줌.
    private final MemberRepository memberRepository;

    /**
     * 회원 가입
     *  - 중복이름 검사
     */
    @Transactional // 따로 @Transactional을 선언하는 경우, 우선권을 가짐 ! readonly=true 설정에서 제외됨
    public Long join(Member member){
        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if(!findMembers.isEmpty())
            throw new IllegalStateException("이미 존재하는 회원 입니다.");
        // 사실 이렇게해도 중복으로 들어가능 장애가 발생할 수 있음.
        // 멀티쓰레드,, 다중 was에 서비스가 올라와있는 경우 동시성 이슈발생
        // => 최후의 방어로직으로 database에도 unique 제약조건을 걸어둬야함.
    }
    // 회원 전체 조회

//    @Transactional(readOnly = true)
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }
//    @Transactional(readOnly = true)
    public Member findOne(Long memberId){
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) {
        // 엔티티를 업데이트할 땐 변경감지로 업데이트할 것!!
        Member member = memberRepository.findOne(id);
        member.setName(name);
        /*
        동작 요약
        1. 트랜잭션 시작
        2. memberRepository.findOne(id)
           : jpa가 영속성 컨텍스트에서 아이디로 찾음 => 없으면 db에서 조회하여 반환
        3. 조회한 엔티티는 영속성 컨텍스트에 등록되어 영속상태가 되고
        4. setName()후 MemberService.update가 종료되면서, Spring의 AOP가 동작하면서,
        5. @Transactional에 의해, 트랜잭션에 관련된 AOP가 끝나는 시점에, 트랜잭션 커밋이 일어남
        6. 그때 JPA가 영속성컨텍스트를 flush하고 데이터베이스 트랜잭션 커밋을 함
        */


    }
}
