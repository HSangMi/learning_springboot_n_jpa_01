package jpabook.jpashop.controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("item", new BookForm());
        return "item/createItemForm";
    }

    @PostMapping("/items/new")
    public String create(BookForm bookForm) {
        Book book = new Book();
        // setter를 제거하는게 좋은 설계 ! 예제니깐 ㅎㅎ
        book.setName(bookForm.getName());
        book.setAuthor(bookForm.getAuthor());
        book.setPrice(bookForm.getPrice());
        book.setIsbn(bookForm.getIsbn());

        itemService.saveItem(book);
        return "redirect:/";
    }

    @GetMapping("/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    // JPA에서 수정을 어떻게할까 ? 변경감지(권장) vs 병합
    @GetMapping("items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") long itemId, Model model) {
        Book item = (Book)itemService.findOne(itemId); // 예제 단순화를 위해 캐스팅해서 쓰겠음. 좋은건아님

        // update할때, Book entity를 보내는게 아니라 BookForm을 보낼것
        BookForm bookForm = new BookForm();
        bookForm.setId(itemId);
        bookForm.setName(item.getName());
        bookForm.setAuthor(item.getAuthor());
        bookForm.setPrice(item.getPrice());
        bookForm.setIsbn(item.getIsbn());
        model.addAttribute("item", bookForm);
        return "items/updateItemForm";
    }

    @PostMapping("items/{itemId}/edit")
    public String updateItem(@PathVariable Long itemId, @ModelAttribute("form")BookForm form){
        Book book = new Book();


        book.setId(form.getId());
        book.setName(form.getName());
        book.setAuthor(form.getAuthor());
        book.setPrice(form.getPrice());
        book.setIsbn(form.getIsbn());

        /* 준영속 엔티티 : DB에 한번 저장되어 식별자가 존재하는 엔티티
            new로 생성했지만, 식별자가 존재함! 그러나 엔티티매니저가 관리하진 않음
            준영속 엔티티를 수정하는 방법 2가지
            1. 변경감지
            2. 병합(merge)
        */
        itemService.saveItem(book);
        /* 어설프게 Controller에서 Entity를 생성하지 말자!
        예제에선 BookForm을 웹계층에서만 쓰기로 정의 했기 떄문에 `new Book()`을 해서 넘겼지만,
        아래가 더 나은 코드!, 업데이트할 데이터가 많다! 싶으면 서비스계층에 DTO를 하나 만들어라
            > itemService.saveItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());
            > itemService.saveItem(itemId, updateItemDto);
        : 트랜젝션이 있는 서비스 계층에 식별자(`id`)와 변경할 데이터를 명확히 전달하는 것이 좋음(파라미터 or Dto)
        : 트랜젝션이 있는 서비스 게층에서 영속상태의 엔티티를 조회하고, 엔티티의 데이터를 직접 변경할것!(merge가 일어나게 하지 마쇼)
           (트랜잭션 안에서 엔티티를 조회 해야, 영속상태로 조회가 되고, 거기에서 값을 변경해야, 더티체킹이 일어나면서 flush하면서 업데이트가 나간다)

        */
        return "redirect:/items";
    }
}
