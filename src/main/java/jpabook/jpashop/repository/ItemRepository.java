package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {
    private final EntityManager em;
    public void save(Item item) {
        if(item.getId() == null) {
            // 최초 생성일 땐 id가 없음 : 신규 생성 객체!
            em.persist(item);
        }else{
            em.merge(item); // update와 비슷! 자세한건 이후에 웹어플리케이션쪽 개발에서 다룸
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }
    public List<Item> findAll() {
        // 여러개 찾는거는 jpql작성 필요~!
        return em.createQuery("select i from Item i", Item.class).getResultList();
    }
}
