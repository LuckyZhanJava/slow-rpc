package com.lonicera.rpc.handler;

import com.lonicera.rpc.client.ClientOptions;
import com.lonicera.rpc.client.RequestOptions;
import com.lonicera.rpc.invoke.MethodInvocation;
import com.lonicera.rpc.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;

public class DefaultHandlerContext implements HandlerContext {

  private Handler protocolHandler;
  private int index;
  private List<Handler> handlerList;
  private RequestOptions options;
  private Server server;

  public DefaultHandlerContext(
      Protocol<?, ?> protocol,
      boolean useSSL,
      ClientOptions clientOptions,
      String host,
      int port,
      RequestOptions requestOptions,
      List<Handler> handlerList
  ) {
    this.server = new Server(host, port);
    this.options = requestOptions;
    this.handlerList = new ArrayList<>(handlerList);
    this.protocolHandler = new ProtocolHandler<>(protocol, useSSL, clientOptions);
  }

  @Override
  public Object handle(MethodInvocation invocation) {
    if (index < handlerList.size()) {
      Handler handler = handlerList.get(index);
      index++;
      return handler.handle(this, server, options, invocation);
    } else {
      return protocolHandler.handle(this, server, options, invocation);
    }
  }

}
