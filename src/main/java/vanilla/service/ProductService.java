package vanilla.service;

import java.util.List;
import java.util.Optional;
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
  public Optional<Product> getByCode(String code) {
    return productRepository.findByCode(code);
  }

  @Transactional(readOnly = true)
  public List<String> getActiveProductCodes(int maxPriceCents) {
    return productRepository.findAll().stream()
        .filter(p -> p.getPriceCents() <= maxPriceCents)
        .map(Product::getCode)
        .collect(Collectors.toList());
  }

  @Transactional
  public Product createProduct(String code, String name, int priceCents) {
    Product product = new Product();
    product.setCode(code);
    product.setName(name);
    product.setPriceCents(priceCents);
    return productRepository.save(product);
  }

  @Transactional
  public Optional<Product> updateProduct(String code, String name, int priceCents) {
    return productRepository
        .findByCode(code)
        .map(
            p -> {
              p.setName(name);
              p.setPriceCents(priceCents);
              return productRepository.save(p);
            });
  }

  @Transactional
  public boolean deleteProduct(String code) {
    productRepository.deleteHistoryByProductCode(code);
    return productRepository.deleteByCodeNative(code) > 0;
  }
}
