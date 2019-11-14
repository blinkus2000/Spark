package unlimited.core.util.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import unlimited.core.io.streams.InputStreamIterator;

public abstract class StreamUtil {
	public static <T1, T2> Stream<T2> collapseWithFunction(Stream<T1> in, BiFunction<T1, T2, Boolean> collectAndContinue, Function<T1,T2> collectorFactory) {
	    Collapser_FunctionalBuilder<T1, T2> collapser = new Collapser_FunctionalBuilder<T1, T2>(in, collectAndContinue, collectorFactory);
	    return collapser.build();
	  }

	public static class Collapser_FunctionalBuilder<T1, T2> {
		private final Iterator<T1> in;
	    private final BiFunction<T1, T2, Boolean> collectAndContinue;
	    private final Function<T1, T2> collectorFactory;
	    private final Iterator<T2> out;

		public Collapser_FunctionalBuilder(Stream<T1> in, BiFunction<T1, T2, Boolean> collectAndContinue, Function<T1, T2> collectorFactory) {
			this.in = in.iterator();
		      this.collectAndContinue = collectAndContinue;
		      this.collectorFactory = collectorFactory;
		      this.out = new StreamCollapse_FunctionalConverter();
		}

		public Stream<T2> build() {
			Iterable<T2> iterable = () -> out;
		    return StreamSupport.stream(iterable.spliterator(), false);
		}
		private final class StreamCollapse_FunctionalConverter implements Iterator<T2> {
		      T2 next = null;

		      @Override
		      public boolean hasNext() {
		        return in.hasNext();
		      }

		      @Override
		      public T2 next() {
		        if (!hasNext()) {
		          throw new NoSuchElementException("There is no next element!");
		        }
		        T1 nextFromIn = in.next();
		        if (next == null) {
		          next = collectorFactory.apply(nextFromIn);
		        }
		        T2 returnVal = next;

		        while (nextFromIn != null) {
		          
				Boolean apply = collectAndContinue.apply(nextFromIn, returnVal);
				if (!apply) {
		            next = null;
		            break;
		          }else if(hasNext()) {
		        	nextFromIn = in.next();  
		          }else {
		        	  nextFromIn = null;
		          }
		        }
		        return returnVal;
		      }

		    }
		  }

	
/**
   * @param in incoming stream to collapse
   * @param shouldFill test made to see if the item can be added via collect, if nothing has been collected and this returns false, the next item in the stream will be a collected item with just a single element in it
   * @param collect method to collect T1 items into T2 collections
   * @param collectorFactory factory method to generate fresh T2 collections
   * @return
   */
  public static <T1, T2> Stream<T2> collapse_preTest(Stream<T1> in,BiPredicate<T1,T2> shouldFill ,BiConsumer<T1, T2> collect, Supplier<T2> collectorFactory) {
    Collapser_PreTestBuilder<T1, T2> collapser = new Collapser_PreTestBuilder<T1, T2>(in, shouldFill,collect, collectorFactory);
    return collapser.build();
  }
  public static class Collapser_PreTestBuilder<T1, T2> {

    private final Iterator<T1> in;
    private final BiPredicate<T1,T2> shouldFill;
    private final BiConsumer<T1, T2> collect;
    private final Supplier<T2> collectorFactory;
    private final Iterator<T2> out;

    public Collapser_PreTestBuilder(Stream<T1> in, BiPredicate<T1,T2> shouldFill, BiConsumer<T1, T2> collect, Supplier<T2> collectorFactory) {
      this.in = in.iterator();
      this.shouldFill = shouldFill;
      this.collect = collect;
      this.collectorFactory = collectorFactory;
      this.out = new StreamCollapse_PreTestConverter();
    }

    public Stream<T2> build() {
      Iterable<T2> iterable = () -> out;
      return StreamSupport.stream(iterable.spliterator(), false);
    }
    private final class StreamCollapse_PreTestConverter implements Iterator<T2> {
      T2 next = null;

      @Override
      public boolean hasNext() {
        //we either have a loaded next or the iterator still has items
        return next!=null||hasNextInner();
      }

      private boolean hasNextInner() {
        return in.hasNext();
      }

      @Override
      public T2 next() {
        if (!hasNext()) {//no next is loaded and in has no next.
          throw new NoSuchElementException("There is no next element!");
        }
        T2 returnVal;
        if(!hasNextInner()) {
          //this means there is nothing in the main stream but we do have a loaded next so we should set the returnVal, null out our loaded next and return the returnVal, the stream is done.
          returnVal = next;
          next = null;
          return returnVal;
        }
        boolean lingeringCurrent = true;
        T1 currentItem = in.next();
        if (next == null) {
          //we have not loaded a next yet, so let's load it 
          next = collectorFactory.get();
          //we should always add at least one item to the 
          collect.accept(currentItem, next);
          lingeringCurrent = false;
        }
        returnVal = next; //establish the returnVal now, at this point no matter what gets loaded into it, we are returning the returnVal.
        if(hasNextInner()&&!lingeringCurrent) {
          //there is no lingering current value, and the main stream has an item to be evaluated, let's extract it and begin collapsing
          currentItem = in.next();
          lingeringCurrent = true;
        }else if(!lingeringCurrent)  {
          //special case, we did have a next item but it was already added when we created the collector factory and loaded it
          next = null;
          return returnVal;
        }
        
        while(shouldFill.test(currentItem, returnVal)) {
          //this is the basic case, we can add the item
          collect.accept(currentItem, returnVal);
          lingeringCurrent = false; //this will be evaluated later once the shouldFill test returns false, 
          if(hasNextInner()) {
            currentItem = in.next(); 
            lingeringCurrent = true;//if this passes shouldFill, it will be added otherwise we will exit the while loop and this current item will be lingering
          }else {
            break;
          }
        }
        if(lingeringCurrent) {
          //this means the current item has not been collected
          //next call to hasNext() will return true and a new next containing the current item will be waiting
          next = collectorFactory.get();
          collect.accept(currentItem, next);
          lingeringCurrent = false; //not needed but it keeps track
        }else {
          //We have nothing left, it was all added
          next = null;
        }
          
        return returnVal;
      }
    }
  }
  /**
   * @param in - incoming stream to expand;
   * @param collectAndContinue - define a collection where the T1 is added to the T2 and if, after adding the item it
   *          can still accept items, return true, if after adding it cannot accept new items, return false
   * @param collectorFactory - define a factory method for the T2 collector
   * @return returns a Stream<T2> that is a collapsed mapping of the T1 in stream
   */
  public static <T1, T2> Stream<T2> collapse(Stream<T1> in, BiFunction<T1, T2, Boolean> collectAndContinue, Supplier<T2> collectorFactory) {
    Collapser_PostTestBuilder<T1, T2> collapser = new Collapser_PostTestBuilder<T1, T2>(in, collectAndContinue, collectorFactory);
    return collapser.build();
  }
  
  public static class Collapser_PostTestBuilder<T1, T2> {

    private final Iterator<T1> in;
    private final BiFunction<T1, T2, Boolean> collectAndContinue;
    private final Supplier<T2> collectorFactory;
    private final Iterator<T2> out;

    public Collapser_PostTestBuilder(Stream<T1> in, BiFunction<T1, T2, Boolean> collectAndContinue, Supplier<T2> collectorFactory) {
      this.in = in.iterator();
      this.collectAndContinue = collectAndContinue;
      this.collectorFactory = collectorFactory;
      this.out = new StreamCollapse_PostTestConverter();
    }

    public Stream<T2> build() {
      Iterable<T2> iterable = () -> out;
      return StreamSupport.stream(iterable.spliterator(), false);
    }

    private final class StreamCollapse_PostTestConverter implements Iterator<T2> {
      T2 next = null;

      @Override
      public boolean hasNext() {
        return in.hasNext();
      }

      @Override
      public T2 next() {
        if (!hasNext()) {
          throw new NoSuchElementException("There is no next element!");
        }
        if (next == null) {
          next = collectorFactory.get();
        }
        T2 returnVal = next;

        while (hasNext()) {
          if (!collectAndContinue.apply(in.next(), returnVal)) {
            next = null;
            break;
          }
        }
        return returnVal;
      }

    }
  }

  public static <T1, T2> Stream<T2> expand(Stream<T1> in, Function<T1, Stream<T2>> mapper) {
    return new ExpanderBuilder<T1, T2>(in, mapper).build();
  }

  public static class ExpanderBuilder<T1, T2> {

    private final Iterator<T1> in;
    private final Function<T1, Stream<T2>> mapper;
    private final Iterator<T2> out;
    private Iterator<T2> currentOut = null;
    private AutoCloseable toBeClosed = null;
    public ExpanderBuilder(Stream<T1> in, Function<T1, Stream<T2>> mapper) {
      this.in = in.iterator();
      this.mapper = mapper;
      this.out = new StreamExpanderConverter();
    }

    public Stream<T2> build() {
      Iterable<T2> iterable = () -> out;
      return StreamSupport.stream(iterable.spliterator(), false);
    }

    private final class StreamExpanderConverter implements Iterator<T2> {

      @Override
      public boolean hasNext() {
       if(currentOut!=null&&currentOut.hasNext()) {
         return true;
       }
       
       currentOut = null;
       while(currentOut==null&&in.hasNext()) {
         if(toBeClosed!=null) {
           try {
             toBeClosed.close(); 
           } catch(Exception e) {
             e.printStackTrace();
           }
         }
         Stream<T2> apply = mapper.apply(in.next());
         toBeClosed = apply;
        Iterator<T2> tempOut = apply.iterator();
         if(tempOut.hasNext()) {
           currentOut = tempOut;
           return true;
         }
       }
       return false;
      }

      @Override
      public T2 next() {
        if (hasNext()) {
          return currentOut.next();
        }
        else {
          throw new NoSuchElementException("There is no next element!");
        }

      }
    }
  }

  public static Stream<ByteBuffer> buildByteBufferStream(InputStream in, final int bufferSize, final Supplier<ByteBuffer> fact){
    Stream<ByteBufferWrapper> wrapperStream = buildByteBufferWrapperStream(in, bufferSize, fact);
    return wrapperStream.map(wrap->wrap.buf);
  }
  public static Stream<ByteBufferWrapper> buildByteBufferWrapperStream(InputStream in, final int bufferSize, final Supplier<ByteBuffer> fact) {
    IntStream intStream = InputStreamIterator.getByteStream(in);
    Supplier<ByteBufferWrapper> supplier = ()->new ByteBufferWrapper(fact.get());
    Stream<ByteBufferWrapper> wrapperStream = collapse(intStream.boxed(),(intVal,wrap)->{
      wrap.write(intVal);
      return wrap.currentPosition<(bufferSize);
    },supplier);
    return wrapperStream;
  }
  public static class ByteBufferWrapper{
    final ByteBuffer buf;
    int currentPosition = 0;
    ByteBufferWrapper(ByteBuffer buf){
      this.buf = buf;
    }
    void write(int val) {
      this.buf.array()[currentPosition] = (byte) val;
      currentPosition++;
    }
    public ByteBuffer getBuf() {
      return buf;
    }
    public int size() {
      return currentPosition;
    }
    
  }
}
