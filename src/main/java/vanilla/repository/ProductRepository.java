package vanilla.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vanilla.entity.Product;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByCode(String code);
    Product findByNameLikeIgnoreCase(String name);
    List<Product> findAllByCodeIn(Collection<String> codes);
}
