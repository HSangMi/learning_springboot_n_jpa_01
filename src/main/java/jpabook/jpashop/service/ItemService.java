package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true) // 기본적으로 저장 안되게 설정
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }
    /* JPA에서 update 하는방법1 : 변경감지 (Best Practice) */
    @Transactional
    public void updateItem(Long itemId, Book param) {
        // id로 실제 DB있는 영속상태 엔티티를 찾아옴
        Item findItem = itemRepository.findOne(itemId);
        // set을 통해 직접바꾸지 말고, entity에 의미있는 메소드로 엔티티 내에서 처리해야, 유지보수 시 변경 추적을 하기 쉬움 ex. addStock, change..
        // => setter없이 엔티티 안에서 바로 추적 가능한 메서드를 만드는게 유지보수하기 좋음!!
        findItem.setPrice(param.getPrice());
        findItem.setName(param.getName());
        findItem.setStockQuantity(param.getStockQuantity());
        //  itemRepository.save(findItem);  // save, persist 등등 호출할 필요없음!!!!
        /*
            이미 findItem으로 영속상태 엔티티를 가져왔고,
            @Transctional에 의해, 트랜잭션이 커밋됨!
            이 JPA는 커밋되는 시점에 flush를 날림!
            flush : 영속성 컨텍스트에 등록된 엔티티중에 변 경감지를 하고 update 함
         */

    /* JPA에서 update 하는 방법2 : 병합(Merge) */
    /*
        병합 : 준영속 상태 엔티티를 변경할 때 사용하는 기능
        saveItem -> save에서 식별자(id)값이 있으면 위에 작성한 코드와 동일하게
        식별자로 영속성 엔티티를 가져와서
        파라미터로 넘어온 객체로 값을 바꿔치기 한 후, 변경감지를 일으켜 업데이트 하는방식

        * 디테일한 동작 순서*
        1. 준영속 엔티티를 save->merge할 경우, 식별자로 영속엔티티 조회(1차캐시 조회 -> 없으면 DB조회)
        2. 새로 찾아온 영속성엔티티(mergeMember)에 파라미터로 넘어온 준영속 엔티티 데이터로 바꿔치기 한뒤 return 함
            > Item merge = em.merge(item);
            > item : 영속상태x, merge : 영속상태 o => 이후 값을 변경하기 위해선 `merge`객체를 사용해야함.

        * 주의 *
        - 변경 감지 기능을 사용하면, 원하는 속성만 선택해서 변경 할 수 있지만,
        - 병합을 사용하면 모든 속성이 변경된다.(선택x)
        - 병합 시 값이 없으면, `null`로 업데이트 할 위험도 있다.(병합 시 모든 필드를 교체하기 때문)
        => 실무에선 귀찮더라도, 변경 필요한 필드들만 setting하여  변경감지를 사용할 것!
    */
    }

    public List<Item> findItems(){
        return itemRepository.findAll();
    }
    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }


}
