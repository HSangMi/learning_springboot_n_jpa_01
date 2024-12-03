package jpabook.jpashop.repository.order.query;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * query / SimpleQuery 패키지를 나눈 이유
 * 핵심 비즈니스 로직을 처리하기 위해 엔티티를 찾을 떈 SimpleQuery로 찾고
 * query는 화면이나 API에 의존적인 조회를 위해 부가적인 것들이 함께 포함될 때로 구분하여 관리하기 위함
 *
 * 화면의 api에 의해 돌아가는 라이프사이클과 핵심비즈니스 로직을 처리하기 위한 라이프 사이클이 좀 많이 다르기때문에 분리하는 것 추천!
 *
 */


@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    // 컬렉션을 포함해 조회해 올 경우
    public List<OrderQueryDto> findOrderQueryDtos() {
        // 루트쿼리 1번 조회
        List<OrderQueryDto> result = findOrders();
        // 컬렉션 N번 조회
        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });
        /**
         * ToOne(N:1, 1:1)관계는 먼저 조회하고, ToMany(1:N)관계는 별도로 처리한다.
         *  => ToOne관계는 join해도 row수가 증가하지 않지만, ToMany관계는 join 할 경우 row수가 증가함
         *  => ToOne관계는 조인으로 최적화가 잘됨 -> 한번에 조회, ToMany는 join 최적화가 어려우므로 별도로 조회
         *
         * => 결국 쿼리가 N+1만큼 나감
         */

        return result;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {

        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        // oi.order.id 이런식으로 연결된 객체의 값을 꺼내올 수 있음. 이경우엔 fk라 굳이 order를 찾지않고 가져오긴 함.!
                " OrderItem oi" +
                " join oi.item i" +
                " where oi.orderId = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId",orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, o.name, o.orderDate, o.status, d.address)" +
                        // 이렇게 jpql로 생성자에 바로 넣더라도, collection을 넣을 수 는 없음. 데이터를 flat하게 한줄로 밖에 못 넣음
                        " Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderQueryDto.class
        ).getResultList();
    }

    // 컬렉션 조회 최적화 해보기!!
    public List<OrderQueryDto> findAllByDto_optimiztion() {
        // Order 조회는 똑같음
        List<OrderQueryDto> result = findOrders();
        // 루프를 돌지않고, 한방에가져와 보겠음
        List<Long> orderIds = toOrderIds(result);

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;

        /**
         * 쿼리는 루트쿼리 1 + 컬렉션쿼리 1 해서 총 두번나감.
         * 한번에 가져와서 메모리에서 처리
         */
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                // oi.order.id 이런식으로 연결된 객체의 값을 꺼내올 수 있음. 이경우엔 fk라 굳이 order를 찾지않고 가져오긴 함.!
                                " OrderItem oi" +
                                " join oi.item i" +
                                " where oi.orderId in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream().collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
        return orderItemMap;
    }

    private static List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream().map(o -> o.getOrderId()).collect(Collectors.toList());
    }
}
