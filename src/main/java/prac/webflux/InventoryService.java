package prac.webflux;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class InventoryService {

    private ItemRepository itemRepository;

    private CartRepository cartRepository;

    public InventoryService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    public Mono<Cart> getCart(String cartId) {
        return this.cartRepository.findById(cartId);
    }

    public Flux<Item> getInventory() {
        return this.itemRepository.findAll();
    }

    Flux<Item> searchByEaxmple(String name, String description, boolean useAnd) {
        Item item = new Item(name, description, 0.0);

        ExampleMatcher matcher = (useAnd
            ? ExampleMatcher.matchingAll()
            : ExampleMatcher.matchingAny())
            .withStringMatcher(StringMatcher.CONTAINING)
            .withIgnoreCase()
            .withIgnorePaths("price");

        Example<Item> probe = Example.of(item,matcher);

        return itemRepository.findAll(probe);
    }

    Mono<Cart> addItemToCart(String cartId, String itemId) {
        return this.cartRepository.findById(cartId)
            .defaultIfEmpty(new Cart(cartId))
            .flatMap(cart -> cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getItem().getId().equals(itemId))
                .findAny()
                .map(cartItem -> {
                    cartItem.increment();
                    return Mono.just(cart);
                })
                .orElseGet(() -> {
                    return this.itemRepository.findById(itemId)
                        .map(item -> new CartItem(item))
                        .map(cartItem -> {
                            cart.getCartItems().add(cartItem);
                            return cart;
                        });
                }))
            .flatMap(cart -> this.cartRepository.save(cart));
    }
}
