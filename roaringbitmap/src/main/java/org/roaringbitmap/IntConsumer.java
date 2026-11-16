package org.roaringbitmap;

/**
 * An IntConsumer receives the int values contained in a data structure.
 * Each value is visited once.
 *
 * Usage:
 *
 * <pre>
 * {@code
 *  bitmap.forEach(new IntConsumer() {
 *
 *    public void accept(int value) {
 *      // do something here
 *
 *    }});
 *   }
 * }
 * </pre>
 *
 * <p>This interface extends {@link java.util.function.IntConsumer}, so instances can be passed to
 * JDK APIs (for example {@code IntStream.forEach}) and to the library methods that accept a
 * {@link java.util.function.IntConsumer}, without wrapping.
 */
public interface IntConsumer extends java.util.function.IntConsumer {
  /**
   * Receives the integer
   *
   * @param value the integer value
   */
  @Override
  void accept(int value);
}
