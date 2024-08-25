package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.item.Item;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Category {
    @Id @GeneratedValue
    @Column(name ="category_id")
    private Long id;

    private String name;

    /*
     * Items <-> Categories
        - many to many 관계를 보여주기 위한 예제, 단방향으로 해도 되지만 양방향으로 해볼게
     * M:N 관계를 표현하려면
        - 객체의 경우는 양쪽에서 Collection 관계를 표현할 수 있지만,
        - 관계형 DB는 일대다 - 다대일 로 풀어내는 중간테이블(category_item)이 필요!
        => @JoinTable로 중간테이블 로 매핑 생성
        => 실전에서 사용하지 않는 이유는, 중간테이블 기본형태(단순매핑) 외에, 다른 필드를 추가하거나 할 수 없음.
    */
    @ManyToMany
    @JoinTable(name ="category_item",
            joinColumns = @JoinColumn(name ="category_id"), // 중간테이블(category_item)에 있는 category_id 컬럼
            inverseJoinColumns = @JoinColumn(name="item_id") //item에 해당하는 id 맵핑
    )
    private List<Item> items = new ArrayList<>();

    // 카테고리 계층구조를 표현해보자 : 혼자서 양방향 매핑
    // 부모 카테고리
    @ManyToOne(fetch = FetchType.LAZY) // 자식N : 부모 1
    @JoinColumn(name = "parent_id") // 매핑할 컬럼명(fk)
    private Category parent;

    // 자식 카테고리
    @OneToMany(mappedBy="parent")   // 맵핑된 필드명(객체)
    private List<Category> child = new ArrayList<>();

    //== 연관관계 메서드 ==//
    public void addChildCategory(Category category) {
        child.add(category);
        category.setParent(this);
    }

}
