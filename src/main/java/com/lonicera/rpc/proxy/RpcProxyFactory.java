package com.lonicera.rpc.proxy;

import com.lonicera.rpc.client.ClientOptions;
import com.lonicera.rpc.client.RequestOptions;
import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.handler.DefaultHandlerContext;
import com.lonicera.rpc.handler.Handler;
import com.lonicera.rpc.handler.HandlerContext;
import com.lonicera.rpc.invoke.DefaultMethodInvocation;
import com.lonicera.rpc.invoke.MethodInvocation;
import com.lonicera.rpc.protocol.Protocol;
import com.lonicera.rpc.server.RpcContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

public class RpcProxyFactory {

  @Data
  @AllArgsConstructor
  private class SchemeProtocol {

    private boolean useSSL;
    private Protocol<?, ?> protocol;
  }

  private static final RequestOptions REQUEST_OPTIONS = new RequestOptions();
  private static final ClientOptions CLIENT_OPTIONS = new ClientOptions();
  private List<Protocol<?, ?>> protocolList;
  private Map<String, SchemeProtocol> schemeProtocolMap;

  public RpcProxyFactory(List<Protocol<?, ?>> protocolList) {
    this.protocolList = protocolList;
    this.schemeProtocolMap = mapProtocolList(protocolList);
  }

  private Map<String, SchemeProtocol> mapProtocolList(List<Protocol<?, ?>> protocolList) {
    Map<String, SchemeProtocol> protocolMap = new HashMap<>();
    for (Protocol<?, ?> protocol : protocolList) {
      protocolMap.put(protocol.name(), new SchemeProtocol(false, protocol));
      protocolMap.put(protocol.name() + "s", new SchemeProtocol(true, protocol));
    }
    return protocolMap;
  }

  public <T> T getProxy(Class<T> clazz, String uri) {
    return getProxy(clazz, uri, CLIENT_OPTIONS);
  }

  public <T> T getProxy(Class<T> clazz, String uri, ClientOptions clientOptions
  ) {
    return getProxy(clazz, uri, clientOptions, REQUEST_OPTIONS);
  }

  public <T> T getProxy(Class<T> clazz, String uri, ClientOptions clientOptions,
      RequestOptions requestOptions
  ) {
    return getProxy(clazz, uri, clientOptions, requestOptions, Collections.emptyList());
  }

  @AllArgsConstructor
  private static class RawObject {

    Object factory;
    Class<?> clazz;
    String uri;
  }

  public <T> T getProxy(Class<T> clazz, String uri, ClientOptions clientOptions,
      RequestOptions options,
      List<Handler> handlerList) {
    if (!clazz.isInterface()) {
      throw new IllegalArgumentException("require interface");
    }
    URI uriObject = URI.create(uri);
    String scheme = uriObject.getScheme();
    SchemeProtocol schemeProtocol = schemeProtocolMap.get(scheme);
    if (schemeProtocol == null) {
      throw new IllegalArgumentException("unsupported scheme : " + scheme);
    }
    RawObject rawObject = new RawObject(this, clazz, uri);
    InvocationHandler invocationHandler = invocationHandler(
        rawObject,
        schemeProtocol.protocol,
        schemeProtocol.useSSL,
        clientOptions,
        uriObject.getHost(),
        uriObject.getPort(),
        options,
        handlerList
    );
    Object o = Proxy
        .newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, invocationHandler);
    return (T) o;
  }

  private static final Object[] EMPTY_ARGS = new Object[0];


  private InvocationHandler invocationHandler(
      RawObject rawObject,
      Protocol<?, ?> protocol,
      boolean useSSL,
      ClientOptions clientOptions,
      String host,
      int port,
      RequestOptions requestOptions,
      List<Handler> handlerList
  ) {
    return new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
          try {
            return method.invoke(rawObject, args);
          } catch (IllegalAccessException e) {
            throw new RpcException(RpcException.CLIENT_ERROR, e);
          } catch (InvocationTargetException e) {
            throw new RpcException(RpcException.CLIENT_ERROR, e.getCause());
          }
        }

        int actualPort = port > 0 ? port : (useSSL ? 443 : 80);
        HandlerContext handlerContext = new DefaultHandlerContext(
            protocol,
            useSSL,
            clientOptions,
            host,
            actualPort,
            requestOptions,
            handlerList);
        MethodInvocation invocation = new DefaultMethodInvocation(method,
            args == null ? EMPTY_ARGS : args);
        try {
          return handlerContext.handle(invocation);
        } finally {
          RpcContext.clearContext();
        }
      }
    };
  }
}
