package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue
    @Column(name="order_item_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id ")
    private Item item;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") // 매핑할 컬럼명(fk)
    private Order order;

    private int orderPrice; // 주문 당시가격
    private int count; // 주문수량

    //== 생성 메소드 ==//
//    protected OrderItem(){
//        // jpa는 protected까지 기본생성자를 사용할 수 있음. jpa에서 protected는 쓰지말라는 의미로 선언 ㅎㅎ
//        // 지정한 createOrderItem메소드 외에 외부에서 다른 생성패턴을 막기위해 ! 선언해둠 !!
//        // 롬복에서도 지원!! @NoArgsConstructor(access = AccessLevel.PROTECTED)
//    }
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);
        return orderItem;
    }

    //== 비즈니스 로직==//
    public void cancel() { // 재고 상태를 원복한다
        getItem().addStock(count);
    }
    //== 조회 로직 ==//

    /**
     * 주문 상품 전체 가격 조회
     */
    public int getTotalPrice() {
        return orderPrice*count;
    }
}
