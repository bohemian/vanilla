package vanilla.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vanilla.dto.Quote;
import vanilla.dto.QuoteParams;
import vanilla.entity.Product;
import vanilla.repository.ProductRepository;

@Slf4j
@Service
public class QuoteService {

  private ProductRepository productRepository;

  public QuoteService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Quote getQuote(QuoteParams params) {
    int totalCents = 0;
    log.info("Getting quote for {} products", params.getProductCodeQuantities().size());
    List<Product> products =
        productRepository.findAllByCodeIn(params.getProductCodeQuantities().keySet());
    if (products.size() != params.getProductCodeQuantities().size()) {
      Set<String> foundProductCodes =
          products.stream().map(Product::getCode).collect(Collectors.toSet());
      List<String> invalidProductCodes =
          params.getProductCodeQuantities().keySet().stream()
              .filter(code -> !foundProductCodes.contains(code))
              .toList();
      throw new IllegalArgumentException("Invalid product codes: " + invalidProductCodes);
    }
    Map<String, Product> codeProductMap =
        products.stream().collect(Collectors.toMap(Product::getCode, p -> p));
    for (Map.Entry<String, Integer> productCodeQuantity :
        params.getProductCodeQuantities().entrySet()) {
      var product = codeProductMap.get(productCodeQuantity.getKey());
      totalCents += product.getPriceCents() * productCodeQuantity.getValue();
    }

    return new Quote(totalCents);
  }
}
