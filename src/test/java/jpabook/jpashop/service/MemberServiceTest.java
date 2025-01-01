package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/*
순수 단위 테스트 말고
실제 jpa가 실제 db까지 가는것을 테스트하기위해
메모리모드로 db까지 엮어서 테스트코드를 작성하겠음!~
@RunWith(SpringRunner.class) : 스프링과 intergrated 을 해서 테스트
@SpringBootTest //스프링부트 띄운상태로 테스트. 스프링 컨테이너 안에서 테스트 돌릴수 있게해줌 . @Autowired 같은 걸쓸수있음
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional // 테스트끝나고 롤백하게 해줌
public class MemberServiceTest { // tips 테스트 class 만들기 ctrl + shift + T

    // 테스트케이스니깐 가장 간단하게
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em; // 직접 flush해서 db쿼리 날아가는 거 보기위해 가져옴 => 쿼리볼수있고, 트랜잭션도 롤백됨

    @Test
//    @Rollback(false)
    public void 회원가입() throws Exception{
        //given
        Member member = new Member();
        member.setName("kim");
        //when
        // join -> save -> em.perist(member)가 이루어지는데, persist된다고 db에 insert가 날아가는게 아님!(db마다 전략이 다르긴 한데, 기본적으로 GenerateValue 전략에선 더더욱아님)
        // 데이터베이스 트랜젝션이 커밋 될 때,이 때 플러시가 되면서 db인서트 쿼리가 나감
        // => 트랜젝션 커밋이 더욱 중요해짐!

        // => @Transactional이 롤백을해서 db쿼리가 안날라감
        // => db에 들어가는 것을 볼려면 @Rollback어노테이션을 사용,
        memberService.join(member);
        
        //then
        em.flush();
        assertEquals(member, memberRepository.findById(member.getId()));
    }

    @Test
    public void 중복_회원_예외() throws Exception{
        //given
        Member member = new Member();
        member.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        //when
        memberService.join(member);
        try{
            memberService.join(member2);
        } catch (IllegalStateException e){
            return;
        }
        memberService.join(member2); // 예외가 발생되어야 함!!

        //then
        // Assert.fail : 이 코드에 도달하면 잘못된거임!
        fail("예외가 발생해야 함");
    }
}
/*
외부에 있는 DB로 테스트를 진행했음. 테스트를 여려환경에서 병렬로 돌릴경우, 외부db를 설치해야하는 번거로움이 있음
테스트를 완전히 격리된 환경 : java띄울떄, java안에 살짝 db를 새로 만들어서 띄우는 방법이 있음!
=> 메모리 DB : 스프링부트에서 공짜로 제공!
프로젝트는 기본적으로 main과 test로 디렉토리가 나뉘는데,
main : 실제 개발하는 운영소스! java와 resources
test : java가 있는데, resources 폴더를 하나 만들어주고 main/resources에 있는 application.yml을 복사해옴
(
* 기본적으로 운영로직은(main/java)는 'main/resources`여기 들어있는 내용이 우선권
* test로직은(test/java) 'test/resources'에 있는게 우선권을 가짐
)

build.gradle에 `dependencies{ runtimeOnly:'com.h2database:h2}`
얘가 클라이언트 역할 뿐만 아니라, jvm안에서 띄울 수 있음

https://h2database.com/html/cheatSheet.html
In-Memory 방식으로 띄우는 방법 있음!!

방법 1.
test/resources/application.yml의 datasource url을 h2db의 in-memory모드로 띄우는 url로 입력

방법 2.
test/resources/application.yml의 db설정정보를 다 주석처리함! 기본적으로 springboot는 설정이 없을 경우 in-memory방식을 채택하기 때문

* 참고 : 운영환경과 테스트환경의 설정파일은 따로 가져가는게 맞음!!
*/