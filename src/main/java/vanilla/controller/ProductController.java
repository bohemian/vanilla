package vanilla.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vanilla.entity.Product;
import vanilla.repository.ProductRepository;

@RestController
public class ProductController {

  private final ProductRepository productRepository;

  public ProductController(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @GetMapping("/products")
  public List<String> getProductCodes() {
    return productRepository.findAll().stream()
        .filter(p -> p.getPriceCents() > 0)
        .map(Product::getCode)
        .collect(Collectors.toList());
  }
}
