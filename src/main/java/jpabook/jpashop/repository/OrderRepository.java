package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;
    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    // 검색기능 - 모든 조건으로 검색
    public List<Order> findAll(OrderSearch orderSearch){
        return em.createQuery("select o from Order o join o.member m" +
                " where o.status =:status " +
                " and m.name like :name", Order.class)
                .setParameter("status", orderSearch.getOrderStatus())
                .setParameter("name", orderSearch.getMemberName())
                .setMaxResults(1000)
                .getResultList();
    }

    // 검색기능 jpql을 직접 조건에따라작성 - 무식한방법
    public List<Order> findAllByString(OrderSearch orderSearch) {
        String jpql = "select o from Order o join o.member m where 1=1";

        // 회원이름 검색
        if(StringUtils.hasText(orderSearch.getMemberName())){
            jpql += " and m.name like :name";
        }

        // 주문상태 검색
        if(orderSearch.getOrderStatus()!=null){
            jpql += " and o.status in (:status)";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);
        if(StringUtils.hasText(orderSearch.getMemberName())){
            query.setParameter("name", orderSearch.getMemberName());
        }
        if(orderSearch.getOrderStatus()!=null){
            query.setParameter("status", orderSearch.getOrderStatus());
        }
        return query.getResultList();
    }

    /**
     * JPA Criteria
     * jpa가 제공하는 동적쿼리를 빌드해주는 표준으로제공하는 방법
     * : 딱히 권장x => Querydsl 사용추천
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch){
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문상태 검색
        if(orderSearch.getOrderStatus() != null){
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }
        // 회원 이름 검색
        if(StringUtils.hasText(orderSearch.getMemberName())){
            Predicate name = cb.like(m.<String>get("name"), "%"+orderSearch.getMemberName()+"%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> orderTypedQuery = em.createQuery(cq).setMaxResults(1000);
        return orderTypedQuery.getResultList();
    }
}
