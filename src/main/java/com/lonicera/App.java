package com.lonicera;

import com.lonicera.rpc.client.ClientOptions;
import com.lonicera.rpc.client.RequestOptions;
import com.lonicera.rpc.protocol.Protocol;
import com.lonicera.rpc.protocol.http.HttpProtocol;
import com.lonicera.rpc.proxy.RpcProxyFactory;
import com.lonicera.rpc.server.RpcServer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 */
public class App {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    HttpProtocol httpProtocol = new HttpProtocol();

    RpcServer rpcServer = new RpcServer(httpProtocol, 8910);
    Hello service = new SimpleHello();
    rpcServer.registerService(service);
    rpcServer.start();

    List<Protocol<?, ?>> protocolList = new ArrayList<>();
    protocolList.add(new HttpProtocol());
    RpcProxyFactory proxyFactory = new RpcProxyFactory(protocolList);

    Hello hello1 = proxyFactory.getProxy(Hello.class, "http://127.0.0.1:8910");
    String hi = hello1.hello();
    System.out.println(hi);
    String repeat = hello1.repeat("repeat");
    System.out.println(repeat);
    String concat = hello1.concat("hello", "tomcat");
    System.out.println(concat);
    CompletableFuture<LocalDateTime> future = hello1.future(LocalDateTime.now(), 30);
    System.out.println(future.get());

    String nullVal = hello1.repeat(null);
    System.out.println(nullVal);
    hello1.concat(null, null);

    Hello hello2 = proxyFactory.getProxy(
        Hello.class,
        "http://placeholder",
        new ClientOptions(),
        new RequestOptions()
    );



    rpcServer.shutdown();
  }

  static interface Hello {
    String hello();
    String repeat(String say);
    String concat(String hi, String name);
    CompletableFuture<LocalDateTime> future(LocalDateTime now, int days);
  }

  static class SimpleHello implements Hello{

    @Override
    public String hello() {
      return "hello";
    }

    @Override
    public String repeat(String say) {
      return say;
    }

    @Override
    public String concat(String hi, String name) {
      if(hi == null || name == null){
        throw new IllegalArgumentException("parameter require not null");
      }
      return hi + ":" + name;
    }

    @Override
    public CompletableFuture<LocalDateTime> future(LocalDateTime now, int days) {
      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      return CompletableFuture.completedFuture(now.plusDays(days));
    }
  }

}
