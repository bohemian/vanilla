package vanilla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuoteParams {

  @NotNull private Map<@NotNull String, @NotNull @Min(1) Integer> productCodeQuantities;
}
