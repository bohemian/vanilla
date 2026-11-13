package vanilla.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;

@Data
@Entity
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String code;
  private String name;
  private int priceCents;

  @OneToMany
  @JoinColumn(name = "product_id")
  private List<ProductPriceHistory> employees;
}
