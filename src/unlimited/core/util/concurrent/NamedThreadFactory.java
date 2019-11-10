package unlimited.core.util.concurrent;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
  private String baseName = "FactorySmartThread";
  final static short initialValue = 1;
  Short threadNum = new Short(initialValue);
  ThreadGroup threadGroup = null;
  long stackSize = 0;
  final boolean daemonThreads;
  public NamedThreadFactory(String baseName, boolean daemonThreads) {
    this.baseName = baseName + "-";
    this.daemonThreads = daemonThreads;
  }
  public NamedThreadFactory(String baseName) {
    this(baseName,false);
  }
  public NamedThreadFactory(ThreadGroup threadGroup, String baseName, boolean daemonThreads) {
    this(baseName,daemonThreads);
    this.threadGroup = threadGroup;
  }
  public NamedThreadFactory(ThreadGroup threadGroup, String baseName) {
    this(threadGroup,baseName,false);
  }
  public NamedThreadFactory(ThreadGroup threadGroup, String baseName, long stackSize, boolean daemonThreads) {
    this(threadGroup, baseName,daemonThreads);
    this.stackSize = stackSize;
  }
  public NamedThreadFactory(ThreadGroup threadGroup, String baseName, long stackSize) {
    this(threadGroup,  baseName,  stackSize, false);
  }
  final Object threadNumLock = new Object();
  @Override
  public Thread newThread(Runnable r) {
    StringBuilder threadName = new StringBuilder(baseName);
    Thread thread;
    short number;
    synchronized(threadNumLock) { 
      if(threadNum == Short.MAX_VALUE) threadNum = initialValue; else threadNum ++; 
      number = threadNum;
    }
    String name = threadName.append(number).toString();
    if(stackSize > 0) {
      thread = new Thread(threadGroup, r, name, stackSize);
    }else if(threadGroup != null) {
      thread = new Thread(threadGroup, r, name);
    } else {
      thread = new Thread(r, name);
    }
    thread.setDaemon(daemonThreads);
    return thread;
  }
  
}
