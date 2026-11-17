package vanilla.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vanilla.entity.Product;
import vanilla.repository.ProductRepository;

@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<String> getActiveProductCodes() {
    return productRepository.findAll().stream().map(Product::getCode).collect(Collectors.toList());
  }
}
