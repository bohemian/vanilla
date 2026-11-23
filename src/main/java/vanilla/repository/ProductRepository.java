package vanilla.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vanilla.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
  Optional<Product> findByCode(String code);

  Product findByNameLikeIgnoreCase(String name);

  List<Product> findAllByCodeIn(Collection<String> codes);

  @Modifying
  @Query(
      value =
          "DELETE FROM product_price_history WHERE product_id = (SELECT id FROM product WHERE code = :code)",
      nativeQuery = true)
  void deleteHistoryByProductCode(@Param("code") String code);

  @Modifying
  @Query(value = "DELETE FROM product WHERE code = :code", nativeQuery = true)
  int deleteByCodeNative(@Param("code") String code);
}
