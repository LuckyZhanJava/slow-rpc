package com.lonicera.rpc.utils;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
  private static final AtomicLong atomic = new AtomicLong();
  private IdGenerator(){
  }

  public static long next(){
    return atomic.incrementAndGet();
  }
}
