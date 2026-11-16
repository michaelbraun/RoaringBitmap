/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap.longlong;

/**
 * An LongConsumer receives the long values contained in a data structure. Each value is visited
 * once.
 *
 * Usage:
 *
 * <pre>
 * {@code
 *  bitmap.forEach(new LongConsumer() {
 *
 *    public void accept(long value) {
 *      // do something here
 *
 *    }});
 *   }
 * }
 * </pre>
 *
 * <p>This interface extends {@link java.util.function.LongConsumer}, so instances can be passed to
 * JDK APIs (for example {@code LongStream.forEach}) and to the library methods that accept a
 * {@link java.util.function.LongConsumer}, without wrapping.
 */
public interface LongConsumer extends java.util.function.LongConsumer {
  /**
   * Receives the long
   *
   * @param value the long value
   */
  @Override
  void accept(long value);
}
