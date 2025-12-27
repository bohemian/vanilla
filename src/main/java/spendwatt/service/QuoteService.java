package spendwatt.service;

import org.springframework.stereotype.Service;
import spendwatt.model.ProductPrice;
import spendwatt.model.Product;
import spendwatt.model.ProductPriceRepository;
import spendwatt.model.Quote;
import spendwatt.model.QuoteParams;

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
                throw new IllegalArgumentException("Product " + productQuantity.getKey() + " is not available");
            }
            total += productPrice.get().getCostCents() * productQuantity.getValue();

        }

        return new Quote(total);
    }
}
