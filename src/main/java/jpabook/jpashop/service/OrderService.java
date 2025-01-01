package jpabook.jpashop.service;

import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 # 도메인 모델 패턴(JPA, ORM 사용 시 좋음)
 서비스 계층은 단순히 엔티티에 필요한 요청을 위임하는 역할을 함.
 엔티티가 비즈니스 로직을 가지고, 객체지향의 특성을 적극 활용하는 것
<=>
트랜잭션 스크립트 패턴(SQL작성 했을 때 주로 사용)
반대로, 엔티티에는 비즈니스 로직이 거의 없고, 서비스 계층에서 대부분의 비즈니스 로직을 처리하는 것

* 각자 문맥에 따라 트레이드 오프가 있음
* 한 프로젝트 내에서도 두 패턴이 양립하기도 함.
 */

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    /**
     * 주문
     */
    @Transactional // 데이터를 변경하는 거니깐
    public Long order(Long memberId, Long itemId, int count) {
        // 엔티티 조회
        Member member = memberRepository.findById(memberId).get();
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장
        // order만 persist 했는데, orderItem, delivery도 persist 되는 이유!
        // => cascade 옵션에 설정 했기 때문!
        // cascade 사용 권장 범위 : 프라이빗 오너일경우만 ! 다른 것이 참조할수없는 !
        // (OrderItem과 delivery를 참조하는 주인은 Order 뿐, 라이프 사이클이 동일하게 관리되는 애들끼리만 사용 권장)
        orderRepository.save(order);
        return order.getId();
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 주문 엔티티 조회
        Order order = orderRepository.findOne(orderId);
        // 주  문 취소
        order.cancel();
    }

    public List<Order> findOrders(OrderSearch orderSearch) {
        return orderRepository.findAllByString(orderSearch);
        // 단순 조회, 위임서비스라면, 컨트롤러에서 리포지토리를 호출해도 괜찮다고 봄
    }

}
