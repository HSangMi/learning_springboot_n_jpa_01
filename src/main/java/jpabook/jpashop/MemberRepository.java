package jpabook.jpashop;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

// @Repository : 엔티티를 찾아주는 애 (like dao), 스프링에서 제공하는 컴포넌스 스캔에 대상이되는 어노테이션
@Repository
public class MemberRepository {

    // JPA를 쓰기때문에, 엔티티매니저가 필요!
    // 스프링 부트를 쓰기때문에, 모든게 스프링 컨테이너 위에서 동작
    // @PersistenceContext 애노테이션이 있으면, 스프링이 알아서 엔티티매니저를 주입해줌
    // (엔티티 매너지를 생성하거나 직접 팩토리에서 꺼내올 필요 없음)
    @PersistenceContext
    private EntityManager em;

    public Long save(Member member) {
        em.persist(member);
        return member.getId();
        /*
        * member가 아닌 id만 리턴하는 이유
            * 추천 코드 스타일 : 커맨드와 쿼리를 분리해라
            * 사이드 이팩트를 줄이기 위해, 커맨드 작업 이후에 리턴값을 최소화
            * id정도는 이후에 조회가 필요할 수 있으니 전달
         */
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }

    // Tips : 테스트코드 만들기 단축키 -  shift ctrl T
}
