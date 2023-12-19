package com.lonicera.rpc.client;

import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.protocol.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.FastThreadLocal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

class ChannelRpcRequestHandler extends ChannelDuplexHandler {

  private static AtomicLong atomicLong = new AtomicLong();

  private static FastThreadLocal<HashedWheelTimer> TIMER_THREAD_LOCAL = new FastThreadLocal<HashedWheelTimer>() {
    @Override
    protected HashedWheelTimer initialValue() throws Exception {
      return new HashedWheelTimer(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r);
          t.setDaemon(true);
          t.setName("timeout-thread-" + atomicLong.incrementAndGet());
          return t;
        }
      });
    }
  };

  private static FastThreadLocal<Map<String, Timeout>> READ_TIMEOUT_MAP_LOCAL = new FastThreadLocal<Map<String, Timeout>>() {
    @Override
    protected Map<String, Timeout> initialValue() throws Exception {
      return new ConcurrentHashMap<>();
    }
  };

  private static FastThreadLocal<Map<String, CompletableFuture<RpcResponse>>> RESPONSE_FUTURE_MAP_LOCAL = new FastThreadLocal<Map<String, CompletableFuture<RpcResponse>>>() {
    @Override
    protected Map<String, CompletableFuture<RpcResponse>> initialValue() throws Exception {
      return new ConcurrentHashMap<>();
    }
  };

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof RpcResponse) {
      String correlationId = ((RpcResponse) msg).correlationId();
      Timeout timeout = READ_TIMEOUT_MAP_LOCAL.get().remove(correlationId);
      if (timeout != null) {
        timeout.cancel();
      }
      CompletableFuture<RpcResponse> responseFuture = RESPONSE_FUTURE_MAP_LOCAL.get()
          .remove(correlationId);
      if (responseFuture != null) {
        responseFuture.complete((RpcResponse) msg);
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof ChannelRpcRequest) {
      CompletableFuture<RpcResponse> responseFuture = ((ChannelRpcRequest) msg).getResponseFuture();
      int writeTimeoutMillis = ((ChannelRpcRequest) msg).getWriteTimeoutMillis();
      RpcRequest rpcRequest = ((ChannelRpcRequest) msg).getRpcRequest();
      ChannelFuture writeFuture = ctx.write(rpcRequest, promise);
      Timeout writeTimeout =
          writeTimeoutMillis > 0 ? writeTimeout(ctx, responseFuture, writeTimeoutMillis) : null;
      writeFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            if (writeTimeout != null) {
              writeTimeout.cancel();
            }

            int readTimeoutMillis = ((ChannelRpcRequest) msg).getReadTimeoutMillis();
            if (readTimeoutMillis > 0) {
              Timeout readTimeout = readTimeoutTask(ctx.channel(), responseFuture,
                  readTimeoutMillis);
              READ_TIMEOUT_MAP_LOCAL.get().put(rpcRequest.correlationId(), readTimeout);
            }
            RESPONSE_FUTURE_MAP_LOCAL.get().put(rpcRequest.correlationId(), responseFuture);
          }
        }
      });
    } else {
      super.write(ctx, msg, promise);
    }
  }

  private Timeout readTimeoutTask(Channel channel, CompletableFuture<RpcResponse> responseFuture,
      int readTimeout) {
    Timer timer = TIMER_THREAD_LOCAL.get();
    return timer.newTimeout(new TimerTask() {
      @Override
      public void run(Timeout timeout) throws Exception {
        TimeoutException timeoutException = new TimeoutException(channel.toString());
        responseFuture.completeExceptionally(
            new RpcException(RpcException.READ_TIMEOUT, timeoutException));
      }
    }, readTimeout, TimeUnit.MILLISECONDS);
  }

  private Timeout writeTimeout(ChannelHandlerContext ctx,
      CompletableFuture<RpcResponse> responseFuture, int writeTimeout) {
    if (writeTimeout <= 0) {
      return null;
    }
    Timer timer = TIMER_THREAD_LOCAL.get();
    return timer.newTimeout(new TimerTask() {
      @Override
      public void run(Timeout timeout) throws Exception {
        TimeoutException timeoutException = new TimeoutException(ctx.channel().toString());
        responseFuture.completeExceptionally(
            new RpcException(RpcException.WRITE_TIMEOUT, timeoutException));
      }
    }, writeTimeout, TimeUnit.MILLISECONDS);
  }
}
