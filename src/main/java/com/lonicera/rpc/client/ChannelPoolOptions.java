package com.lonicera.rpc.client;

import io.netty.channel.pool.FixedChannelPool.AcquireTimeoutAction;
import lombok.Getter;

@Getter
public class ChannelPoolOptions {
  private final int maxPoolSize;
  private final int acquireTimeoutMillis;
  private final AcquireTimeoutAction action;
  private final int idleTimeoutMillis;

  public ChannelPoolOptions(){
    this(10, 1000, AcquireTimeoutAction.NEW, 60000);
  }

  public ChannelPoolOptions(int maxPoolSize, int acquireTimeoutMillis, AcquireTimeoutAction action, int idleTimeoutMillis){
    this.maxPoolSize = maxPoolSize;
    this.acquireTimeoutMillis = acquireTimeoutMillis;
    this.action = action;
    this.idleTimeoutMillis = idleTimeoutMillis;
  }

}
