package vanilla.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vanilla.entity.Product;
import vanilla.service.ProductService;

@RestController
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/products")
  public List<String> getProductCodes(@RequestParam int maxPriceCents) {
    return productService.getActiveProductCodes(maxPriceCents);
  }

  @GetMapping("/products/{code}")
  public Optional<Product> getProductByCode(@PathVariable String code) {
    return productService.getByCode(code);
  }
}
