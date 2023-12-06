package com.lonicera.rpc.server;


import com.lonicera.rpc.protocol.Protocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLException;

public final class RpcServer {

  private ServerBootstrap bootstrap;
  private NioEventLoopGroup eventLoopGroup;
  private ServiceRegistry serviceRegistry;
  private int port;
  private AtomicBoolean startupComplete = new AtomicBoolean(false);
  private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
  private AtomicBoolean isStartingUp = new AtomicBoolean(false);
  private int workerThreadCount;
  private ExecutorService executorService;

  public RpcServer(Protocol<?, ?> protocol, int port) {
    this(protocol, port, null, Runtime.getRuntime().availableProcessors());
  }

  public RpcServer(Protocol<?, ?> protocol, int port, int workerThreadCount) {
    this(protocol, port, null, workerThreadCount);
  }

  public RpcServer(Protocol<?, ?> protocol, int port, SslConfig sslConfig) {
    this(protocol, port, sslConfig, Runtime.getRuntime().availableProcessors());
  }

  public RpcServer(Protocol<?, ?> protocol, int port, SslConfig sslConfig, int workerThreadCount) {
    SslContext sslContext = null;
    if (sslConfig != null) {
      try {
        sslContext = SslContextBuilder.forServer(sslConfig.getKeyCertChainInputStream(), sslConfig.getKeyInputStream()).build();
      } catch (SSLException e) {
        throw new IllegalStateException(e);
      }
    }
    this.workerThreadCount = workerThreadCount;
    this.executorService = new ThreadPoolExecutor(
        workerThreadCount,
        workerThreadCount * 2,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(512)
    );
    eventLoopGroup = new NioEventLoopGroup();
    serviceRegistry = new ServiceRegistry();
    SslContext sslCtx = sslContext;
    bootstrap = new ServerBootstrap()
        .channel(NioServerSocketChannel.class)
        .group(eventLoopGroup)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            if (sslCtx != null) {
              pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
            }
            pipeline.addLast()
                .addLast(protocol.decodeRequestHandlers())
                .addLast(protocol.encodeResponseHandlers())
                .addLast(new RpcMethodInvokeHandler<>(protocol, serviceRegistry, executorService))
            ;
          }
        });
    this.port = port;
  }

  public void registerService(Object service) {
    serviceRegistry.register(service);
  }

  public List<Object> getServices(){
    return serviceRegistry.getServices();
  }

  public void start() {
    if (isShuttingDown.get()) {
      throw new IllegalStateException("Server is still shutting down, cannot re-start!");
    }

    if (startupComplete.get()) {
      return;
    }
    try {
      if (isStartingUp.compareAndSet(false, true)) {
        bootstrap.bind(port).syncUninterruptibly();
      }
      startupComplete.set(true);
      isStartingUp.set(false);
    } catch (Exception e) {
      isStartingUp.set(false);
      startupComplete.set(false);
      isShuttingDown.set(false);
      throw e;
    }
  }

  public void shutdown() {
    if (isStartingUp.get()) {
      throw new IllegalStateException("Server is still starting up, cannot shut down!");
    }
    if (isShuttingDown.compareAndSet(false, true)) {
      try {
        eventLoopGroup.shutdownGracefully();
        executorService.shutdown();
        startupComplete.set(false);
        isShuttingDown.set(false);
      } catch (Exception e) {
        isShuttingDown.set(false);
        throw e;
      }
    }
  }

}
