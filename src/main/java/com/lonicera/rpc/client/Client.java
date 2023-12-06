package com.lonicera.rpc.client;

import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.protocol.Protocol;
import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.protocol.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

public class Client {

  private Protocol<?, ?> protocol;
  private String host;
  private int port;
  private ClientOptions clientOptions;
  private Bootstrap bootstrap;
  private ChannelPool channelPool;
  private static final ThreadFactory CLIENT_THREAD_FACTORY = new ThreadFactory() {

    private DefaultThreadFactory factory = new DefaultThreadFactory("rpc-client");

    @Override
    public Thread newThread(@NotNull Runnable r) {
      Thread t = factory.newThread(r);
      t.setDaemon(true);
      return t;
    }
  };

  public Client(Protocol<?, ?> protocol, boolean useSSL, String host, int port, ClientOptions clientOptions) {
    bootstrap = new Bootstrap()
        .channel(NioSocketChannel.class)
        .group(new NioEventLoopGroup(CLIENT_THREAD_FACTORY))
        .remoteAddress(host, port);
    this.host = host;
    this.port = port;
    this.clientOptions = clientOptions;
    channelPool = new FixedChannelPool(bootstrap, new AbstractChannelPoolHandler() {

      @Override
      public void channelCreated(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if(useSSL){
          SslContext sslCtx = SslContextBuilder.forClient().build();
          pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        }
        pipeline
            .addLast(protocol.decodeResponseHandlers())
            .addLast(protocol.encodeRequestHandlers())
            .addLast(new ChannelRpcRequestHandler())
        ;
      }
    }, 10);
  }


  public CompletableFuture<RpcResponse> write(RpcRequest request, RequestOptions options) {
    Future<Channel> channelFuture = channelPool.acquire();

    Channel channel = null;
    try {
      channel = channelFuture.get(clientOptions.getPoolOptions().getAcquireTimeoutMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RpcException(RpcException.CONNECT_ERROR, e);
    }
    ChannelRpcRequest rpcRequest = new ChannelRpcRequest(request, options.getWriteTimeoutMillis(),
        options.getReadTimeoutMillis());
    CompletableFuture<RpcResponse> respFuture = rpcRequest.getResponseFuture();
    channel.writeAndFlush(rpcRequest).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        channelPool.release(future.channel());
        if(future.cause() != null){
          respFuture.completeExceptionally(new RpcException(RpcException.CLIENT_ERROR, future.cause()));
        }
      }
    });
    return respFuture;
  }
}
