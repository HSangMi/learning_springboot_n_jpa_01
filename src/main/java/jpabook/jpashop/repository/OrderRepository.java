package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.Order;
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

    /**
     fetch join을 사용하면, select할 떄 한번에 가져옴(지연로딩 x)
     기술적으론 sql에서 join을 하지만, jpa에서 fetch join으로 명명함
     */
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class
        ).getResultList();
    }

    /**
     * jpa에서  dto로 바로 반환받기
     * new 명령어를 사용해서 jpql의 결과를 DTO로 즉시 반환
     * => 애플리케이션 네트워크용량 최적화(생각보다 미비)
     *      , api 스펙에 맞춘 코드가 repository에 들어가는게 단점
     */
//    public List<OrderSimpleQueryDto> findOrderDto() {
//        return em.createQuery(
//                "select new jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
//                    "from Order o" +
//                    " join o.member m" +
//                    " join o.delivery d", OrderSimpleQueryDto.class)
//                .getResultList();
//    }

    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +   // distinct 키워드를 추가해 줘야 뻥튀기된 Order를 줄여줌
                        " join fetch o.member m" +      // many to one
                        " join fetch  o.delivery d" +   // one to one
                        " join fetch o.orderItem oi" +  // one to many?  데이터가 뻥튀기됨 !! orderItem 기준으로 !! => distinct
                        " join fetch oi.item i ", Order.class)
                /*
                 * # jpa에서의 distinct #
                 * DB의 distinct와 동일한 역할(쿼리에 distinct를 날려줌)
                 *      +
                 * 루트 엔티티가 중복인 경우에 중복을 걸러서 컬렉션에 담아줌
                 *
                 *  DB의 경우엔 조회된 한 row가 모두 일치해야 중복제거가 되는데, jpa에서는 order의 id가 같은 값이면 중복제거해줌


                 *
                 * */

                .getResultList();
    }

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset) // to_One 관계만 fetch join 했기때문에 페이징 가능!
                .setMaxResults(limit)
                .getResultList();
        // jpa.properties.default_batch_fetch_size를 설정하고, _to_one 관계를 생략하고 조회해도 되긴함
        // => default_batch_fetch_size 옵션이 동일하게 적용받기때문에, fetch join해서 가져오던 애들도 in쿼리로 조회 해옴.
        // => 네트워크 비용이 좀더 발생하므로 to_one관계는 미리 fetch join으로 처리하는게 이득!

    }
}
