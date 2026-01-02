package vanilla.service;

import org.springframework.stereotype.Service;
import vanilla.model.ProductPrice;
import vanilla.model.Product;
import vanilla.model.ProductPriceRepository;
import vanilla.model.Quote;
import vanilla.model.QuoteParams;

import java.util.Map;
import java.util.Optional;

@Service
public class QuoteService {

    private ProductPriceRepository productPriceRepository;

    public QuoteService(ProductPriceRepository productPriceRepository) {
        this.productPriceRepository = productPriceRepository;
    }

    public Quote getQuote(QuoteParams params) {
        int total = 0;
        for (Map.Entry<Product, Integer> productQuantity : params.getProductQuantities().entrySet()) {
            Optional<ProductPrice> productPrice = productPriceRepository.findById(productQuantity.getKey());
            if (productPrice.isEmpty()) {
                var prices = productPriceRepository.findAll();
                throw new IllegalArgumentException("Product " + productQuantity.getKey() + " is not available. Try one of: " + prices);
            }
            total += productPrice.get().getCostCents() * productQuantity.getValue();

        }

        return new Quote(total);
    }
}
