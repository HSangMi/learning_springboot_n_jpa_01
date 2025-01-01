package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA
 *  반복되는 중복코드를 자동화 해준다.
 *  (build.gradle에 spring-boot-starter-data-jpa`확인
 *
 *  interface를 만들고 `extends JpaRepository<Type, PkType>`
 *  => 기본적인 CRUD 기능이 모두 제공됨!!
 *  일반화 하기 어려운 기능도, 메서드 이름으로 정확한 JPQL 쿼리를 실행해줌.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 선언만 해두면, jpql을 자동으로 만들어줌!
    // select m from Member m where m.name = ?
    List<Member> findByName(String name);

}
