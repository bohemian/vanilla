package vanilla.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Cache<K, V> {
  final Map<K, V> cache = new HashMap<>();
  final Function<K, V> calculation;

  public Cache(Function<K, V> calculation) {
    this.calculation = calculation;
  }

  V get(K key) {
    if (!cache.containsKey(key)) {
      cache.put(key, calculation.apply(key));
    }
    return cache.get(key);
  }
}
