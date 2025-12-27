package spendwatt;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {
    String toSentenceCase(Enum<?> enumInstance) {
        String name = enumInstance.name();
        return name.charAt(0) + name.substring(1).replace("_", " ");
    }
}
