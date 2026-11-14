package vanilla.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vanilla.entity.Product;
import vanilla.repository.ProductRepository;

@Service
public class ProductService {

  @Autowired private ProductRepository productRepository;

  public List<Product> getAllProducts() {
    System.out.println("Fetching all products");
    return productRepository.findAll();
  }
}
