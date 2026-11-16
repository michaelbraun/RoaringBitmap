package org.roaringbitmap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;
import org.roaringbitmap.longlong.LongConsumer;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * {@link java.util.function.IntConsumer} and {@link java.util.function.LongConsumer} can be passed
 * to forEach / forEachInRange directly, and existing call shapes still resolve to the library
 * consumer overloads.
 */
public class TestJdkFunctionalConsumers {

  // array, run and bitmap containers, plus values across several high keys
  private static RoaringBitmap sample() {
    RoaringBitmap bitmap = RoaringBitmap.bitmapOf(1, 5, 1 << 16, (1 << 16) + 7, -1);
    bitmap.add(200_000L, 210_000L);
    for (int i = 0; i < 10_000; i++) {
      bitmap.add(1_000_000 + 2 * i);
    }
    bitmap.runOptimize();
    return bitmap;
  }

  private static int[] collect(java.util.function.Consumer<java.util.function.IntConsumer> source) {
    IntStream.Builder builder = IntStream.builder();
    java.util.function.IntConsumer jdk = builder::add;
    source.accept(jdk);
    return builder.build().toArray();
  }

  @Test
  public void roaringBitmapAcceptsJdkIntConsumer() {
    RoaringBitmap bitmap = sample();
    assertArrayEquals(bitmap.toArray(), collect(bitmap::forEach));
  }

  @Test
  public void bufferBitmapsAcceptJdkIntConsumer() {
    RoaringBitmap source = sample();
    MutableRoaringBitmap mutable = source.toMutableRoaringBitmap();
    ByteBuffer buffer = ByteBuffer.allocate(mutable.serializedSizeInBytes());
    mutable.serialize(buffer);
    buffer.flip();
    ImmutableRoaringBitmap mapped = new ImmutableRoaringBitmap(buffer);

    assertArrayEquals(source.toArray(), collect(mutable::forEach));
    assertArrayEquals(source.toArray(), collect(mapped::forEach));
  }

  @Test
  public void forEachInRangeAcceptsJdkIntConsumer() {
    RoaringBitmap bitmap = sample();
    int[] expected = IntStream.of(bitmap.toArray()).filter(v -> v >= 3 && v < 205_000).toArray();
    assertArrayEquals(expected, collect(c -> bitmap.forEachInRange(3, 205_000 - 3, c)));
  }

  @Test
  public void roaring64AcceptsJdkLongConsumer() {
    Roaring64Bitmap r64 = new Roaring64Bitmap();
    Roaring64NavigableMap nav = new Roaring64NavigableMap();
    long[] values = {0L, 7L, 1L << 33, (1L << 33) + 5, Long.MAX_VALUE};
    for (long v : values) {
      r64.addLong(v);
      nav.addLong(v);
    }

    LongStream.Builder b1 = LongStream.builder();
    java.util.function.LongConsumer c1 = b1::add;
    r64.forEach(c1);
    assertArrayEquals(r64.toArray(), b1.build().toArray());

    LongStream.Builder b2 = LongStream.builder();
    java.util.function.LongConsumer c2 = b2::add;
    nav.forEach(c2);
    assertArrayEquals(nav.toArray(), b2.build().toArray());

    LongStream.Builder b3 = LongStream.builder();
    java.util.function.LongConsumer c3 = b3::add;
    r64.forEachInRange(1L << 33, 10, c3);
    assertArrayEquals(new long[] {1L << 33, (1L << 33) + 5}, b3.build().toArray());
  }

  @Test
  public void libraryConsumersAreJdkConsumers() {
    int[] sum = {0};
    IntConsumer roaring = v -> sum[0] += v;
    IntStream.of(1, 2, 3).forEach(roaring);
    assertEquals(6, sum[0]);

    long[] lsum = {0};
    LongConsumer roaringLong = v -> lsum[0] += v;
    LongStream.of(1, 2, 3).forEach(roaringLong);
    assertEquals(6, lsum[0]);
  }

  /** Records the class that invoked {@code accept}, to prove the consumer was not wrapped. */
  private static final class CallerRecorder implements java.util.function.IntConsumer {
    final Set<String> callers = new TreeSet<>();

    @Override
    public void accept(int value) {
      // [0] getStackTrace, [1] this method, [2] the direct caller
      callers.add(Thread.currentThread().getStackTrace()[2].getClassName());
    }
  }

  @Test
  public void jdkIntConsumerIsPassedToContainersWithoutWrapping() {
    RoaringBitmap bitmap = sample();
    CallerRecorder heap = new CallerRecorder();
    bitmap.forEach(heap);
    assertEquals(
        new TreeSet<>(
            Arrays.asList(
                ArrayContainer.class.getName(),
                BitmapContainer.class.getName(),
                RunContainer.class.getName())),
        heap.callers);

    MutableRoaringBitmap mutable = bitmap.toMutableRoaringBitmap();
    CallerRecorder buffer = new CallerRecorder();
    mutable.forEach(buffer);
    assertEquals(
        new TreeSet<>(
            Arrays.asList(
                "org.roaringbitmap.buffer.MappeableArrayContainer",
                "org.roaringbitmap.buffer.MappeableBitmapContainer",
                "org.roaringbitmap.buffer.MappeableRunContainer")),
        buffer.callers);
  }

  @Test
  public void containerLibraryConsumerOverloadDelegates() {
    RoaringBitmap bitmap = sample();
    ContainerPointer pointer = bitmap.getContainerPointer();
    while (pointer.getContainer() != null) {
      Container container = pointer.getContainer();
      char key = pointer.key();
      IntStream.Builder viaLibrary = IntStream.builder();
      container.forEach(key, (IntConsumer) viaLibrary::add);
      int[] viaJdk = collect(c -> container.forEach(key, c));
      assertArrayEquals(viaJdk, viaLibrary.build().toArray());
      pointer.advance();
    }
  }

  /** Call shapes that compiled before the JDK overloads were added must still compile. */
  @Test
  public void existingCallShapesStillCompile() {
    RoaringBitmap bitmap = RoaringBitmap.bitmapOf(1, 2, 3);
    List<Integer> seen = new ArrayList<>();

    bitmap.forEach((int x) -> seen.add(x));
    bitmap.forEach(
        new IntConsumer() {
          @Override
          public void accept(int value) {
            seen.add(value);
          }
        });
    IntConsumer roaring = seen::add;
    bitmap.forEach(roaring);
    bitmap.forEach(roaring.andThen(seen::add)); // andThen returns the JDK type
    bitmap.forEachInRange(0, 10, x -> seen.add(x));
    bitmap.forEachInRange(0, 10, seen::add);

    Roaring64Bitmap r64 = Roaring64Bitmap.bitmapOf(1L);
    List<Long> longs = new ArrayList<>();
    r64.forEach(x -> longs.add(x));
    r64.forEach(longs::add);
    r64.forEachInRange(0L, 10, x -> longs.add(x));

    assertEquals(3 * 7, seen.size());
    assertEquals(3, longs.size());
  }
}
