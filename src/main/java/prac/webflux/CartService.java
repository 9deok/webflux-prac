package prac.webflux;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    private ItemRepository itemRepository;
    private CartRepository cartRepository;

    public CartService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    Mono<Cart> addToCart(String cartId, String id) {
        return this.cartRepository.findById(cartId).defaultIfEmpty(new Cart(cartId))
            .flatMap(cart -> cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getItem().getId().equals(id))
                .findAny()
                .map(cartItem -> {
                    cartItem.increment();
                    return Mono.just(cart);
                })
                .orElseGet(() -> this.itemRepository.findById(id)
                    .map(CartItem::new).doOnNext(cartItem -> cart.getCartItems().add(cartItem))
                    .map(cartItem -> cart)))
            .flatMap(this.cartRepository::save);
    }
}
