package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * X to One 관계에 대해 어떻게 성능최적화 하는지 알아보자
 * Order 조회
 * Order -> Member (N:1 => ManyToOne)
 * Order -> Delivery (1:1 => OneToOne)
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 간단한 주문조회 V1 : 엔티티 직접 노출
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> orderV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        // return all;
        /*
        문제 1. 무한루프!!
            b/c : order => member => order => member ...
            sol :양방향 연관관계가 있으면, 한쪽은 @JsonIgnore을 해줘야함
        문제 2. Exception !! : Type Definition error : ByteBuddyInterceptor
            b/c : Order에 Member정보가 Lazy 로딩으로 설정되어 있는데, 즉 DB에서 Member정보를 가져오지 않고
            Member객체를 상속받은 프록시 Member 객체를 생성해서 넣어둠 => ByteBuddy : 요즘 많이쓰는 프록시 라이브러리의 프록시 객체
            : jackson 라이브러리가 멤버정보를 가져오려고하는데 타입이 프록시객체네?!?
            sol : 지연로딩인 경우에는. hibernate 모듈을 설치해서 지연로딩인 객체를 반환하지않도록함
                    or 강제 lazy 로딩을 시키는 옵션을 활성화함
        => BUT, Entity를 직접 반환하는건 많은 문제점들이 있음!! 불필요한 정보노출 이슈, 성능이슈,, 등등
         */

        // 필요한 연관관계만 강제 로딩 시키는 방법
        for(Order order : all) {
            //  order.getMember() : 여기까진 프록시객체
            //  .getName()을 하면 강제 초기화 됨
            order.getMember().getName();    // Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화
        }
        return all;

        /*
        주의사항
        그렇다고 LAZY -> EAGER로 바꾸면 안됨
        em.find() 같이 pk로 조회하는 경우에나 하이버네이트가 성능최적화를 해주지,
        orderRepository.findAllByString 이렇게 만든건 jpql이 sql로 변역되어 그대로 나가기 때문에,
        먼저 Order만 가져온 뒤, EAGER이네 ?? 하고 단건 조회를 날림 => N+1문제도 발생함
        또한 다른 api에서는 Member정보가 필요가 없는데도 가져오기때문에 성능최적화가 어려움
         */
    }
    /**
     * 간단한 주문조회 V2 : 엔티티를 DTO로 반환
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
        return result;
        /* 문제 !! Order를 조회했지만, member, delivery 정보를 각각 조회하면서 쿼리가 너무많이나감
            1번 : 주문 N개가 조회됨
            루프를 돌면서 회원정보 N번, 배송정보 N번의 추가 쿼리가 나감
            => N+1 문제 !! => 성능이슈!!
        */
    }

    // api 스펙을 명확히 규정해야함
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        // dto가 파라미터로 entity를 받는건 크게 문제되진 않음!
        public SimpleOrderDto(Order order){
            this.orderId = order.getId();
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getMember().getAddress();
        }
    }

    /**
     * 간단한 주문조회 V3 : fetch조인으로 성능 최적화
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        // 쿼리가 1개만 나감!!!
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(order -> new SimpleOrderDto(order))
                .collect(Collectors.toList());
        return result;
        /* 아쉬운점 !! DB에서 조회 시 불필요한 컬럼들까지 모두 가져옴 */
    }

    /**
     * 간단한 주문조회 V4 : JPA에서 바로 DTO로 가져오기
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
//        return  orderRepository.findOrderDto();
        return orderSimpleQueryRepository.findOrderDto();
    }

    /**
     * V3과 V4는 트레이드 오프가 있음
     * V4가 성능을 좀 더 최적화 하긴함..요즘은 네트워크 속도가 워낙 빨라서,, 크진 않음
     * V4에서 JPA가 DTO로 바로 반환하게 하는 경우, 해당 쿼리는 재사용이 거의 안됨. 또 api스펙에 맞춘 코드가 repository에 들어가는게 단점
     * V4에서 조회한 객체는 영속성객체가 아니기 때문에 데이터 변경도 할 수 없음
     *
     * => 기본 Repository는 Entity를 조회하는데만 쓰고, 별도 성능 최적화용 쿼리 Respository를 파서 관리하는 방법 추천!
     * => jpabook.jpashop.repository.order.simpleQuery
     */

    /**
     * ### 정리 ###
     * 쿼리 방식 선택 권장 순서
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택
     * 2. 필요하면 fetch join으로 성능을 최적화 한다 => 대부분의 성능이슈 해결
     * 3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 sql이나 스프링 jdbc template을 사용해서 sql을 직접 사용한다
     */
}
