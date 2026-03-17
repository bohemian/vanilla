package vanilla.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ProductPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne()
    @JoinColumn(name = "forumId")
    private int productId;
    private String name;
    private int priceCents;
}
//
//product_id integer not null references product (id),
//started_at timestamp(0) not null default now(),
//ended_at timestamp(0) not null default timestamp '2111-11-11 11:11:11',
//price_cents integer not null