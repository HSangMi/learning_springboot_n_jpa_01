package jpabook.jpashop.domain.item;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)// 상속관계 전략을 부모클래스에 설정해줘야함
@DiscriminatorColumn(name = "dtype") // 어떤 컬럼으로 자식클래스를 구분할지 선언
@Getter @Setter
public abstract class Item {

    @Id
    @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    @ManyToMany(mappedBy = "items") // mappedBy : 거울이에요!, Categroy에 items를 맵팽한거에요!
    private List<Category> categories = new ArrayList<>();
}
