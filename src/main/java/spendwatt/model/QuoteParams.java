package spendwatt.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class QuoteParams {

    @NotBlank
    private Map<Product, Integer> productQuantities;
}
