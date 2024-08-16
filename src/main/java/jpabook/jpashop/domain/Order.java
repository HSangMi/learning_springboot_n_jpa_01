package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="orders")   // order by 절 때문에, 관례로 orders 사용
@Getter @Setter
public class Order {
    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    // 다대일 관계
    @ManyToOne
    @JoinColumn(name ="member_id")  // 매핑을 뭘로한거냐 (FK id - 테이블의 컬럼이름으로)
    private Member member;
    /*
    주문테이블 - 멤버
    멤버 테이블 - orders
    객체는 변경포인트가 2군데지만, 테이블 관점에선 FK 값만 바뀌면됨
    => 연관관계 주인 : FK가 가까운곳에 설정
        : 어떤 값이 변경됐을 때, 쟤의 FK를 바꿀거야?
       멤버가 바꼈는데 Order테이블의 멤버값이 변하는건 이상하자나
    연관관계주인은 해줄거없고, 거울 필드에서 mappedby 추가
     */
    @OneToMany(mappedBy="order")    // OrderItem객체의 order필드에 의해 맵핑됨
    private List<OrderItem> orderItem = new ArrayList<>();

    /*
    JPA의 one to one 관계에서, fk는 어디있어도 상관없음.
    access를 자주하는곳에 fk를 두는게 괜찮음 : 주로 주문에 관련된 주소정보를 호출함
    Order에 delivery의 id를 fk로 가짐! => Order가 연관관계의 주인
    */
    @OneToOne
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // Date타입을 사용할 땐, 날짜관련 jpa 어노테이션 맵핑이 필요했지만,
    // java8의 LocalDateTime을 사용하면, 하이버네이트가 자동으로 지원해줌
    private LocalDateTime orderDate;   // 주문시간

    // 주문상태를 나타내는 enum
    private OrderStatus status; // ORDER, CANCLE
}