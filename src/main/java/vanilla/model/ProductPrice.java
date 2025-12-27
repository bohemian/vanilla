package vanilla.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "product_price")
@Data
public class ProductPrice {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "product")
    private Product product;
    private int costCents;
}
