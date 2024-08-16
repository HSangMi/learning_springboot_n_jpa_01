package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable // jpa 내장타입
@Getter // @Setter  // 값 타입은 Setter x => 변경 불가하게 설계되어야함
                    // => 생성시점에만 설정할 수 있도록!
public class Address {

    private String city;
    private String street;
    private String zipcode;


    // JPA 내부적으로 리플렉션, 프록시 같은 기술을 사용하기 위해선 기본생성자가 필요함 (protected까지 허용)
    protected Address() {}

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
