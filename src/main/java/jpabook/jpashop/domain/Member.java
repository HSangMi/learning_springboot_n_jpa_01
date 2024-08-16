package jpabook.jpashop.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    //Embedded or Embeddable 하나만 해줘도 되긴함.
    @Embedded // 내장타입을 포함하고있다는 어노테이션
    private Address address;

    // 일대다 : 한 사람이 여러 주문을 함.
    @OneToMany(mappedBy = "member") // mappedBy : 연관관계 주인이 아니에요! 매핑된 거울이에요!
    // Order 객체의 member 필드에 의해 매핑된거야! => 읽기전용
    private List<Order> orders = new ArrayList<>();
}
