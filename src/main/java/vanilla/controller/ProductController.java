package vanilla.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @PostMapping("/products")
  public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
    if (productService.getByCode(request.code()).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    Product created =
        productService.createProduct(request.code(), request.name(), request.priceCents());
    URI location = URI.create("/products/" + created.getCode());
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/products/{code}")
  public ResponseEntity<Product> updateProduct(
      @PathVariable String code, @RequestBody ProductRequest request) {
    return productService
        .updateProduct(code, request.name(), request.priceCents())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/products/{code}")
  public ResponseEntity<Void> deleteProduct(@PathVariable String code) {
    return productService.deleteProduct(code)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}
