package vanilla.service;

import org.springframework.stereotype.Service;
import vanilla.entity.Product;
import vanilla.dto.Quote;
import vanilla.dto.QuoteParams;
import vanilla.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private ProductRepository productRepository;

    public QuoteService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Quote getQuote(QuoteParams params) {
        int totalCents = 0;
        List<Product> products = productRepository.findAllByCodeIn(params.getProductCodeQuantities().keySet());
        if (products.size() != params.getProductCodeQuantities().size()) {
            Set<String> foundProductCodes = products.stream().map(Product::getCode).collect(Collectors.toSet());
            List<String> invalidProductCodes = params.getProductCodeQuantities().keySet().stream().filter(code -> !foundProductCodes.contains(code)).toList();
            throw new IllegalArgumentException("Invalid product codes: " + invalidProductCodes);
        }
        Map<String, Product> codeProductMap = products.stream().collect(Collectors.toMap(Product::getCode, p -> p));
        for (Map.Entry<String, Integer> productCodeQuantity : params.getProductCodeQuantities().entrySet()) {
            var product = codeProductMap.get(productCodeQuantity.getKey());
            totalCents += product.getPriceCents() * productCodeQuantity.getValue();
        }

        return new Quote(totalCents);
    }
}
