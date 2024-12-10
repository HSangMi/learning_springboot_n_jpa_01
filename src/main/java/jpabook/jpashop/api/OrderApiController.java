package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * X to Many : 컬렉션 조회 최적화
 */

@RestController
@RequiredArgsConstructor
public class OrderApiController {

     private final OrderRepository orderRepository;
     private final OrderQueryRepository orderQueryRepository;

    /** V1: 엔티티 그대로반환  */
     @GetMapping("/api/v1/orders")
     public List<Order> ordersV1(){
         List<Order> all = orderRepository.findAllByString(new OrderSearch());
         for (Order order : all) {
             // 지연로딩 강제 초기화
             order.getMember().getName();
             order.getDelivery().getAddress();
             List<OrderItem> orderItems = order.getOrderItem();
             orderItems.stream().forEach(o-> o.getItem().getName()); // orderItems에 item도 강제초기화
         }
        return all;
     }

    /** V2: DTO로 감쌈   */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
         List<Order> orders = orderRepository.findAllByString(new OrderSearch());
         // orders -> orderDto로 변환
         List<OrderDto> collect = orders.stream()
                 .map(o -> new OrderDto(o)) // 람다 레퍼런스 표현식 : .map(OrderDto::new)
                 .collect(Collectors.toList());
         return collect;
     }

    /** V3 : 패치조인으로 최적화  => 쿼리 한방!   */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
       List<Order> orders = orderRepository.findAllWithItem();

        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .toList();  //.collect(Collectors.toList())
        return collect;

        /** SimpleQuery 쓰는거랑 똑같은거 아닌가?! ㄴㄴ
         *
         * 페치조인의 심각한 단점!!
         * 1. 페이징이 안됨!!! 메모리 페이징처리 하기때문
         *    XtoOne은 페치조인 + 페이징 OK
         *    XtoMany는 페치조인에서 페이징 XXX
         * 2. 컬렉션 페치조인은 1개만 사용 가능
         *    컬렉션(one To Many)이 둘 이상 페치조인을 하면, 데이터가 부정합하게 조회될 수 있음
         * */
    }

    /** V3.1 : 패치조인 + default_batch_fetch_size 최적화
     * 1. 000_to_One 관계는 모두 fetch join 으로 묶음 (조회 row가 증가하는 관게가 이니기떄문)
     * 2. 000_to_Many 관계는 지연로딩되도록 놔둠
     *    - default_batch_fetch_size 옵션을 설정하여, 한번에 지연로딩이 되도록 설정
     *    - 지연로딩 해올 때 in 쿼리로 1번에서 조회해온 id값을 넣어 한번에 가져오는데, 이떄 in의 최대 갯수를 의미
     *
     *    ex: 1번쿼리 Orders : A001, A002, A003 조회
     *          2번쿼리 OrderItems : where oi.orderId in (A0001, A0002, A0003)
     *      => default_batch_fetch_size를 잘 조절하면, 지연로딩 시 한방에 데이터를 다 떙겨올 수 있음
     *      => pk 기반 in절은 최적화가 아주 잘되는 쿼리이기 떄문에, 1000개를 로딩해서 10번루프를 돌더라도 굉장히 빠르다!
     */

    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value="offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .toList();
        return collect;
    }

    /** V3 vs V3.1
     * V3의 경우 모든 컬렉션을 fetch join을 걸어서, 쿼리는 한번에 나가지만, 중복된 데이터가 많이 발생한다.
     * (일대다 조인은 데이터 조회시 일의 데이터가 다만큼 뻥튀기되서 조회되니깐)
     * => DB => 애플리케이션으로 전송하는 데이터 자체가 많아짐
     *
     * V3.1의 경우 데이터가 중복없이 조회됨 => 전송량이 최적화됨
     *
     * jpa.properties.default_batch_fetch_size의 경우 글로벌 옵션!! 더 세부적으로 정의하고싶으면 @BatchSize(size=1000)사용
     * - collection의 경우(ex : OrderItems), collection 변수에 선언
     * - 아닌경우(ex : to_one관계 - Item) Class 위에 선언
     */

    /** 정리!!
     * 1. to_One(OneToOne, ManyToOne)관계를 모두 fetch join 해옴
     *      (to_One관계는 row수를 증가시키지 않기 떄문에 페이징 쿼리에 영향을 주지 않음)
     * 2. 컬렉션은 지연로딩으로 조회
     * 3. 지연로딩 성능최적화(N+1문제)를 위해 `hibernate.default_batch_fetch_size`, `@BatchSize`를 적용
     *      - hibernate.default_batch_fetch_size : 글로벌 설정
     *      - @BatchSize : 개별 최적화
     *      - 컬렉션이나 프록시객체를 한꺼번에 in 쿼리로 조회(설정한 사이즈만큼)
     *
     * ** 장점!!
     * 1. 쿼리 호출 수가 1+N -> 1+1로 최적화됨
     * 2. 조인보다 DB 데이터 전송량이 최적화 된다(x_toMany 조인 시 중복조회해서 전달하기 떄문) - 미미하긴 함.
     * 3. 컬렉션 fetch join은 페이징이 불가능 하지만, 이 방법은 페이징이 가능하다!!!!!!!
     *
     * * 참고 batch_size 는 100 ~ 1000 사이로 (1000이상은 오류내는 db가 있음)
     * 100보단 1000이 조회시 루프를 덜 돌아서 이득이 있지만, 1000으로 설정할 경우, 데이터를 땡겨올때 데이터베이스, 어플리케이션 부하가 순간적으로 확 증가하게됨
     * => DB든 애플리케이션이든 순간부하를 어디까지 견딜 수 있는 지로 결정하면 된다.
     * =>(CPU와 같은 리소스관점) 경험상 was랑 db가 순간부하에 대해 걱정이 있으면 100정도로 두고 써보면서 늘려가는것도 좋은 방법
     * => (WAS 메모리 입장에서) batchSize가 작으면 좋을까? => 그건 아님! 일반적으로 to_One으로 조회해온 데이터들에 대해 전체루프를 돌릴거니깐 어차피 메모리를 전체 조회해 올떄까지 기다려야함
     * ( 잘라서 조회해올거면 DB조회시 페이징으로 걸러야지, 어플리케이션까지 와서 거를일은 거의 없음), 즉 out of memory가 날 확률은 size 100이나 1000이나 거의 같음
     *
     */

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        return orderQueryRepository.findAllByDto_optimiztion();
    }
    @GetMapping("/api/v6/orders")
    public List<OrderFlatDto> ordersV6(){
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats;
    }

    /**
     * 총정리!!
     *
     * ** 권장 순서 **
     * 1. 엔티티 조회 방식으로 접근(엔티티조회 후 DTO로 변환해서 반환)
     *  1-1. 페치 조인으로 쿼리수를 최적화
     *  1-2 컬렉션 최적화
     *      1-2-1. 페이징이 필요한경우, @BatchSize로 최적화
     *      1-2-2. 페이징이  필요없는경우 페치 조인 사용!
     * 2. 엔티티 조회로 안될경우, DTO 방식으로 조회
     * 3. DTO 조회방식도 안되면, NativeSQL or 스프링 jdbcTemplate
     *
     * ** 엔티티 조회방식의 장점 **
     * - 페치조인 사용 가능, batchSize 옵션을 통해 코드를 거의 수정하지 않고 성능최적화를 시도 할 수 있다
     * - JPA가 많은부분을 최적화 해주기 때문에, 단순한 코드를 유지하면서, 성능을 최적화 할 수 있다.
     * - DTO 조회방식은 SQL을 직접 다루는 것과 유사하기 떄문에, 둘 사이(성능 최적화 vs 코드복잡도)에 줄타기를 해야함.
     *
     *      - 참고 : 엔티티는 직접 캐싱을 하면 안된다!! 하려면 DTO를 해야지!!
     *            => 영속성 컨텍스트에서 관리 되고 있기 때문에, 캐시에 잘못올라가있으면,,, 골치아파짐
     *            => 엔티티를 캐싱하는 방법 중 Hibernate 2차 캐시가 있긴한데, 실무에 적용하기 어려움!
     *
     * ** DTO 조회 방식의 선택지 **
     * - DTO로 조회하는 방법도 각각 장단이 있음
     * - V4의 경우 코드가 단순, 특정 단건조회 일 때는 이 방식이 좋다.(1+N)
     * - V5의 경우, V4에 비해 성능 최적화 (1+1), 코드는 더 복잡
     * - V6는 (V4,V5와 완전히 다른방식)데이터를 중복해서 가져오는 것과 쿼리를 한번만 요청하는 것 사이의 트레이드 오프가 있음. 페이징 불가
     *
     */


    @Getter // 또는 @Data
     static class OrderDto{

         private Long orderId;
         private String name;
         private LocalDateTime orderDate;
         private OrderStatus orderStatus;
         private Address address;
         private List<OrderItemDto> orderItems;

         public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();

            /** 1번 */
//          orderItems = order.getOrderItem(); // 이렇게 하면 null이 반환됨. lazy로딩하는 엔티티기 떄문에

             /** 2번*/
//             order.getOrderItem().stream().forEach(o -> o.getItem().getName()); // 프록시 초기화 후 조회하면 로딩됨
//             orderItems = order.getOrderItem();
             /*
             DTO안에 엔티티가 있으면 안됨! 래핑도 안됨!
             => 엔티티 스펙이 노출되는 것 자체가 지양해야하는 부분
             => DTO는 엔티티에 대한 의존을 완전히 끊어야 함
             */

             /** 3번 */
             // orderItem도 Dto로 만들어 완전히 엔티티와의 종속을 끊을 것!
             orderItems = order.getOrderItem().stream()
                     .map(orderItem -> new OrderItemDto(orderItem))
                     .collect(Collectors.toList());

         }
         @Getter
         public class OrderItemDto{
             // 노출하고 싶은 정보만
             private String itemName;
             private int orderPrice;
             private int count;

             public OrderItemDto(OrderItem orderItem) {
                 itemName = orderItem.getItem().getName();
                 orderPrice = orderItem.getOrderPrice();
                 count = orderItem.getCount();
             }
         }
     }
}
