package vanilla.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

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
