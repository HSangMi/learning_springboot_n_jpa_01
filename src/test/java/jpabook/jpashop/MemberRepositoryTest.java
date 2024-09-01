package jpabook.jpashop;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class) // junit한테 Spring 관련된 테스트라는 것을 명시
@SpringBootTest // springboot로 테스트를 돌려야함.
public class MemberRepositoryTest {
    // MemberRepository가 잘 동작하는지 테스트해보자
    @Autowired
    MemberRepository_practice memberRepository;

    // Tips : tdd - given-when-then live templates에 추가해서 사용!
    @Test
    @Transactional // entityManager를 통한 모든 데이터 변경은 Transation 안에서 이루어져야함
    // * @Transactional이 Spring과 java공식에서(javax, jakarta) 제공하는게 있는데, Spring에서 제공하는 옵션이 더 많음!
    @Rollback(false)// * Test에서 @Transasctional은 기본적으로 테스트가 끝난 후 rollback 함
    public void testMember() throws Exception {
        // given
        Member_practice memberPractice = new Member_practice();
        memberPractice.setUsername("memberA");

        // when
        Long savedId = memberRepository.save(memberPractice);
        Member_practice findMemberPractice = memberRepository.find(savedId);

        //then
        Assertions.assertThat(findMemberPractice.getId()).isEqualTo(memberPractice.getId());
        Assertions.assertThat(findMemberPractice.getUsername()).isEqualTo(memberPractice.getUsername());

        // Q. 저장한 거랑 조회한거랑 같을까? 다를까?
        Assertions.assertThat(findMemberPractice).isEqualTo(memberPractice);
        // A. 같은 트랜잭션 안에서, 저장을 하고 조회를 했기때문에, 영속성 컨텍스트가 똑같음
        // 같은 영속성 컨텍스트 안에서는 ID 값이 똑같으면, 같은 엔티티로 식별( 조회쿼리조차 나가지않음, 1차 캐시에서 꺼내옴 )

        System.out.println("findMember = " + findMemberPractice);
        System.out.println("member = " + memberPractice);
    }
}