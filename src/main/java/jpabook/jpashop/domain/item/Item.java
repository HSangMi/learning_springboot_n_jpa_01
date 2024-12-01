package jpabook.jpashop.domain.item;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Category;
import jpabook.jpashop.exception.NotEnoughStockException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@BatchSize(size=100)
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

    //== 비즈니스 로직 ==//
    /*
    이곳에 재고 관리 로직을 넣는 이유!
    도메인 주도 설계라고 할 때, 이 엔티티 자체가 해결할 수 있는 것들은
    엔티티 안에 비즈니스 로직을 넣는 것이 좋음! => 객체지향적

    이전에는 itemService같은 곳에서 item의 stock을 꺼내와 값을 조정한뒤,
    setStock을 하는 형태로 개발했을거임 => 비추
    데이터를 가지고있는 쪽에 비즈니스 메소드를 가지고 있는 것이 객체지향적으로 응집력을 높일 수 있음
    */

    /**
     * stock 증가
     */
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    /**
     * stock 감소
     * @param quantity
     */
    public void removeStock(int quantity) {
        // 범위 체크 로직 필요
        int restStock = this.stockQuantity - quantity;
        if(restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }


}
