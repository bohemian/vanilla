package vanilla.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class QuoteParams {

    @NotNull
    private Map<@NotNull Product, @NotNull @Min(1) Integer> productQuantities;
}
