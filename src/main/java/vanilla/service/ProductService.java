package vanilla.service;

import java.util.List;
import org.springframework.stereotype.Service;
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
}
