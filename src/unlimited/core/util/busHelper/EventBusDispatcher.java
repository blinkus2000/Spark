package unlimited.core.util.busHelper;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Defaults;
import com.google.common.collect.Sets;

import unlimited.core.util.concurrent.NamedThreadFactory;

public abstract class EventBusDispatcher<T> implements InvocationHandler {
  protected T innerDelegateToDispatch = null;
  final ScheduledThreadPoolExecutor busScheduler;
  final boolean asynchExecution;
  final Class<T> interfaceType;
  private final boolean hasTimeout;
  private final long timeout;
  private final TimeUnit timeoutTimeUnit;
  static boolean debug = false; 
  final static HashSet<Method> BASIC_OBJECT_METHODS = buildBasicObjectMethods();
  public static HashSet<Method> buildBasicObjectMethods() {
    try {
      return Sets.newHashSet(Object.class.getMethod("toString"),Object.class.getMethod("hashCode"), Object.class.getMethod("equals", Object.class));
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return Sets.newHashSet();
  }
  /**
   * @param interfaceType must be a PUBLIC INTERFACE
   * @param poolSize
   * @param asynchExecution
   * @param hasMethodTimout Synchronous methods can timeout and return null 
   * @param timeout timeout for synchronous methods
   * @param timeoutTimeUnit TimeUnit for timeout
   */
  private EventBusDispatcher(Class<T> interfaceType ,int poolSize, boolean asynchExecution, long threadKeepAliveInSeconds,boolean hasMethodTimeout,long timeout,TimeUnit timeoutTimeUnit){
    this.hasTimeout = hasMethodTimeout;
    this.timeout = timeout;
    this.timeoutTimeUnit = timeoutTimeUnit;
    if(interfaceType == null || !interfaceType.isInterface()){
      throw new IllegalArgumentException("parameter1 'interfaceType' must be a valid interface!");
    }
    this.interfaceType = interfaceType;
    busScheduler = new ScheduledThreadPoolExecutor(poolSize);
    busScheduler.setThreadFactory(new NamedThreadFactory(interfaceType.getSimpleName()+" BusDispatch"));
    busScheduler.setKeepAliveTime(threadKeepAliveInSeconds, TimeUnit.SECONDS);
    busScheduler.allowCoreThreadTimeOut(true);
    this.asynchExecution = asynchExecution;
  }
  private EventBusDispatcher(EventBusDispatchBuilder<T> builder) {
    this(builder.interfaceType ,builder.poolSize, builder.asynchExecution, builder.threadKeepAliveInSeconds,builder.hasMethodTimeout,builder.timeout,builder.timeoutTimeUnit);
  }
  public EventBusDispatcher(Class<T> interfaceType ,int poolSize, boolean asynchExecution, long threadKeepAliveInSeconds) {
    this( interfaceType , poolSize,  asynchExecution,  threadKeepAliveInSeconds,false,0l,TimeUnit.MILLISECONDS);
  }
  public EventBusDispatcher(Class<T> interfaceType ,int poolSize, boolean asynchExecution,boolean hasMethodTimeout,long timeout,TimeUnit timeoutTimeUnit) {
    this(interfaceType,poolSize,asynchExecution,1,hasMethodTimeout,timeout,timeoutTimeUnit);
  }
  public EventBusDispatcher(Class<T> interfaceType ,int poolSize, boolean asynchExecution) {
    this(interfaceType , poolSize,  asynchExecution,false,0l,TimeUnit.MILLISECONDS);
  }
  
  public void shutdown() {
      busScheduler.shutdown();
  }
  /**
   * This will wait until all current dispatched events have been processed
   * It will not take into consideration any events after this has been called.
   * @throws InterruptedException
   */
  public final void waitForDispatchedEvents() throws InterruptedException{
    final Runnable runnable = new Runnable() {      
      @Override
      public void run() {
      }
    };
    final Future<?> submit = submitDirect(runnable);
    try {
      submit.get();
    } catch (ExecutionException e) {
    }
  }

  /**
   * @param runnable task to be executed
   * @return future that can be waited upon for completion
   */
  public Future<?> submitDirect(final Runnable runnable) {
    final Future<?> submit = busScheduler.submit(runnable);
    return submit;
  }
  /**
   * @param callable task to be executed
   * @return future that can be waited upon for completion
   */
  public <T2> Future<T2> submitDirect(final Callable<T2> callable) {
    final Future<T2> submit = busScheduler.submit(callable);
    return submit;
  }
  
  public ScheduledFuture<?> schedule(Runnable paramRunnable, long paramLong, TimeUnit paramTimeUnit) {
    return busScheduler.schedule(paramRunnable, paramLong, paramTimeUnit);
  }

  public <V> ScheduledFuture<V> schedule(Callable<V> paramCallable, long paramLong, TimeUnit paramTimeUnit) {
    return busScheduler.schedule(paramCallable, paramLong, paramTimeUnit);
  }

  public ScheduledFuture<?> scheduleAtFixedRate(Runnable paramRunnable, long paramLong1, long paramLong2, TimeUnit paramTimeUnit) {
    return busScheduler.scheduleAtFixedRate(paramRunnable, paramLong1, paramLong2, paramTimeUnit);
  }

  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable paramRunnable, long paramLong1, long paramLong2, TimeUnit paramTimeUnit) {
    return busScheduler.scheduleWithFixedDelay(paramRunnable, paramLong1, paramLong2, paramTimeUnit);
  }

 
 
  /**
   * @return a proxy instance of T that will dispatch all events to the delegate via AsycEventBus 
   */
  @SuppressWarnings("unchecked")
  public final T buildProxy(){
    Class<?>[] interfaces =  {interfaceType};
    return (T)Proxy.newProxyInstance(interfaceType.getClassLoader(), interfaces, this);
  }
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if(BASIC_OBJECT_METHODS.contains(method)){
      return method.invoke(this.getDelegateNoSynchronized(), args);
    }
    DispatchedEvent evt = getDispatchedEvent(method, args);
    final boolean returningVoid = method.getReturnType().equals(Void.TYPE);
    final boolean asynchAnnotation = method.isAnnotationPresent(Synchronous.class);
    if(asynchAnnotation||(!asynchExecution||!returningVoid)){
      return evt.getResponse();
    }
    return null;
  }

  private final DispatchedEvent getDispatchedEvent(Method method, Object[] args) {
    DispatchedEvent evt = buildEvent(method, args);
    final Future<Object> task = busScheduler.submit(evt);
    evt.task = task;
    return evt;
  }
  protected DispatchedEvent buildEvent(Method method, Object[] args) {
    DispatchedEvent evt = new DispatchedEvent(method, args);
    return evt;
  }
  /**
   * @return an initialized delegate, this will be called once.
   */
  protected abstract T initializeDelegate();
  /**
   * override if you need to do anything to a delegate before each use
   */
  protected void sanitizeDelegate() {}
  synchronized T getDelegate(){
    return getDelegateNoSynchronized();
  }

  public T getDelegateNoSynchronized() {
    if(innerDelegateToDispatch==null){
      innerDelegateToDispatch = initializeDelegate();
    }
    sanitizeDelegate();
    return innerDelegateToDispatch;
  }
  public class DispatchedEvent implements Callable<Object> {
    public Future<Object> task;
    final Method method;
    final Object[] args;
    final StackTraceElement[] stackTrace;
    public DispatchedEvent(Method method, Object[] args) {
      this.method = method;
      this.args = args;
      stackTrace = Thread.currentThread().getStackTrace();
    }

    public Object getResponse() throws Throwable {
      try {
        if (hasTimeout) {
          try {
            return task.get(timeout, timeoutTimeUnit);
          } catch (java.util.concurrent.TimeoutException e) {
            if (debug) {
              System.out.println(this.method + " has timed out, cancelling the task");
            }
            try {
              task.cancel(true);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
            final Class<?> returnType = method.getReturnType();
            return Defaults.defaultValue(returnType);
          }
        }
        else {
          return task.get();
        }

      } catch (InterruptedException e) {
        try {
          task.cancel(true);
        } catch (Exception e1) {
          e1.printStackTrace();
        }
        return null;
      } catch (ExecutionException e) {
        try {
          throw e.getCause();
        } catch (WrappedThrowableException e1) {
          throw e1.t;
        }
      }
    }
  
    @Override
    public Object call() throws Exception{
        try {
          return method.invoke(getDelegate(), args);
        } catch (InvocationTargetException e) {
          throw new WrappedThrowableException(e.getTargetException());
        }
    }
    
  }
  @SuppressWarnings({ "unused", "serial" })
  private static class WrappedThrowableException extends Exception{
    final Throwable t;

    public WrappedThrowableException(Throwable t) {
      super();
      this.t = t;
    }
    
  }
  /**
   * @param class1 interface <T> being used for proxy MUST BE PUBLIC
   * @param supplier creates an implementation of the given interface <T>
   * @return proxy interface of type <T>
   */
  public static <T> T wrap(Class<T> class1, Supplier<T> supplier) {
    return buildDefault(class1,supplier,1).buildProxy();
  }
  /**
   * @param class1 interface <T> being used for proxy MUST BE PUBLIC
   * @param supplier creates an implementation of the given interface <T>
   * @return EventBusDispatcher<T> of type <T>
   */
  public static <T> EventBusDispatcher<T> buildDefault(Class<T> class1, Supplier<T> supplier, long secondsToTimeout){
    return new EventBusDispatcher<T>(class1,1,true, secondsToTimeout) {
      @Override
      protected T initializeDelegate() {
        return supplier.get();
      }
    };
  }
  /**
   * @param class1 interface <T> being used for proxy MUST BE PUBLIC
   * @param supplier creates an implementation of the given interface <T>
   * @return EventBusDispatcher<T> of type <T>
   */
  public static <T> EventBusDispatcher<T> buildDefault(Class<T> class1, Supplier<T> supplier){
    return buildDefault(class1,supplier,1);
  }
  public static <T> EventBusDispatchBuilder<T> getBuilder(Class<T> interfaceType, Supplier<T> supplier){
    return new EventBusDispatchBuilder<>(interfaceType, supplier);
  }
  public static class EventBusDispatchBuilder<T>{
    private final Class<T> interfaceType;
    private final Supplier<T> supplier;
    private int poolSize = 1;
    private boolean asynchExecution = true;
    private long threadKeepAliveInSeconds = 1l;
    private boolean hasMethodTimeout = false;
    private long timeout = 0l;
    private TimeUnit timeoutTimeUnit = TimeUnit.MILLISECONDS;
    public EventBusDispatchBuilder(Class<T> interfaceType, Supplier<T> supplier) {
      super();
      this.interfaceType = interfaceType;
      this.supplier = supplier;
    }
    public EventBusDispatchBuilder<T> setPoolSize(int poolSize) {
      this.poolSize = poolSize;
      return this;
    }
    public EventBusDispatchBuilder<T> setAsynchExecution(boolean asynchExecution) {
      this.asynchExecution = asynchExecution;
      return this;
    }
    public EventBusDispatchBuilder<T> setThreadKeepAliveInSeconds(long threadKeepAliveInSeconds) {
      this.threadKeepAliveInSeconds = threadKeepAliveInSeconds;
      return this;
    }
    public EventBusDispatchBuilder<T> setHasMethodTimeout(boolean hasMethodTimeout) {
      this.hasMethodTimeout = hasMethodTimeout;
      return this;
    }
    public EventBusDispatchBuilder<T> setTimeout(long timeout) {
      this.timeout = timeout;
      return this;
    }
    public EventBusDispatchBuilder<T> setTimeoutTimeUnit(TimeUnit timeoutTimeUnit) {
      this.timeoutTimeUnit = timeoutTimeUnit;
      return this;
    }
    
    public  EventBusDispatcher<T> build(){
      return new EventBusDispatcher<T>(this) {
        @Override
        protected T initializeDelegate() {
          return supplier.get();
        }
      };
    }
  } 
  /**
   * If you want a void returning method in your interface, while running in asynch mode, to run synchronously, use this annotation on the void method
   * It does nothing to methods that do not return void
   *
   */
  @Retention(RUNTIME)
  @Target({METHOD})
  @Inherited
  public @interface Synchronous {

  }
}
