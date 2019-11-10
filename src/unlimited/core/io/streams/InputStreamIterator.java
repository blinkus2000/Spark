package unlimited.core.io.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InputStreamIterator implements PrimitiveIterator.OfInt {
  boolean readForward = false;
  private final InputStreamReader in;
  private int readDuringHasNext = -1;
  public InputStreamIterator(InputStream in) {
    this.in = new InputStreamReader(in);
  }
  @Override
  public boolean hasNext() {
    if(!readForward) {
      readForward = true;
      try {
        readDuringHasNext = this.in.read();
      } catch (IOException e) {
        readDuringHasNext = -1;
      }
    }
    return readDuringHasNext!=-1;
  }

  @Override
  public int nextInt() {
    if(readForward) {
      readForward = false;
      return readDuringHasNext;
    }
    try {
      return this.in.read();
    } catch (IOException e) {
      return -1;
    }
  }
  public static IntStream getByteStream(final InputStream in){
    return StreamSupport.intStream(Spliterators.spliteratorUnknownSize(new InputStreamIterator(in), Spliterator.NONNULL), false);
  }
  public static InputStream getInputStream(final Stream<Integer> in) {
    final Iterator<Integer> iterator = in.iterator();
    return new InputStream() {
      @Override
      public int read() throws IOException {
        if(iterator.hasNext()) {
          return iterator.next();
        }
        return -1;
      }};
  }
}
