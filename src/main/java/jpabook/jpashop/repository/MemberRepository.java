package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // ComponentScan에 의해 자동으로 SpringBean으로 등록
@RequiredArgsConstructor
public class MemberRepository {

    // JPA가 제공하는 표준 애노테이션 => 스프링이 entity Manager를 만들어서 주입해줌
    // @PersistenceContext
    // private EntityManager em;
    // 직접 주입받고싶으면 팩토리에서 꺼내써도 됨
    // @PersistenceUnit
    // private EntityManagerFactory emf;

    // 원래 EntityManager는 @PersistenceContext를 사용해야 주입 가능하지만,
    // Spring Data JPA를 사용하면, @Autowired를 지원해줌. (최신 버전에선 springboot에서 지원해줄 수도?)
    // => @Autowired private EntityManager em; 가능
    // => final 키워드 추가하고 @RequiredArgsConstructor 사용가능
    // 다른 레이어들과 동일하게 @RequiredArgsConstructor를 사용하여 일관성 있는 코드 작성 가능
    private final EntityManager em;

    public void save(Member member) {
        // 영속성 컨텍스트에 member객체 등록
        // persist하는 순간 key값(pk)이 항상 생성되어있는게 보장됨!(db에들어가지 않은 순간에도)
        // 트랜잭션이 commit 되는 시점에, db에 반영됨(insert query) 날아감
        em.persist(member);
    }
    public Member findOne(Long id) {
        // jpa가 제공하는 find 메소드 사용
        return em.find(Member.class, id); // 단건 조회,(type, pk)
    }
    public List<Member> findAll() {
        // createQuery로 jpql 작성
        return em.createQuery("select m from Member m", Member.class) // (jpql, 반환타입)
                .getResultList(); // tips : inline 단축키 ctrl + alt + N
        // sql : 테이블 대상으로 쿼리작성
        // jpql : entity(객체) 대상으로 쿼리 작성
    }
    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }

}
