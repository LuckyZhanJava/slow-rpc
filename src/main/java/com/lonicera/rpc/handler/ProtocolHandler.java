package com.lonicera.rpc.handler;

import com.lonicera.rpc.client.Client;
import com.lonicera.rpc.client.ClientOptions;
import com.lonicera.rpc.client.RequestOptions;
import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.invoke.MethodInvocation;
import com.lonicera.rpc.protocol.Protocol;
import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.protocol.RpcResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class ProtocolHandler<REQ extends RpcRequest, RESP extends RpcResponse> implements Handler {

  private Protocol<REQ, RESP> protocol;
  private boolean useSSL;
  private ClientOptions clientOptions;
  private Map<Server, Client> serverClientMap;

  public ProtocolHandler(Protocol<REQ, RESP> protocol, boolean useSSL, ClientOptions clientOptions) {
    this.protocol = protocol;
    this.useSSL = useSSL;
    this.clientOptions = clientOptions;
    this.serverClientMap = new HashMap<>();
  }

  @Override
  public Object handle(HandlerContext context, Server server, RequestOptions options, MethodInvocation invocation) {

    REQ request = null;
    try {
      request = protocol.encodeInvocation(invocation);
    } catch (Exception e) {
      throw new RpcException(RpcException.CLIENT_ERROR, e);
    }

    Client existClient = serverClientMap.get(server);

    Client client = existClient != null ? existClient : newClient(server);

    CompletableFuture<RESP> respFuture = (CompletableFuture<RESP>) client.write(request, options);

    Class<?> returnType = invocation.getMethod().getReturnType();
    if (Future.class.isAssignableFrom(returnType)) {
      CompletableFuture<Object> valueFuture = new CompletableFuture<>();
      respFuture.whenComplete((resp, e) -> {
        if (e != null) {
          if(e instanceof RpcException){
            valueFuture.completeExceptionally(e);
          }else {
            valueFuture.completeExceptionally(new RpcException(RpcException.CLIENT_ERROR, e));
          }
        } else {
          Object value = null;
          try {
            value = protocol.decodeReturnValue(invocation.getMethod(), resp);
            valueFuture.complete(value);
          } catch (Exception ex) {
            if(ex instanceof RpcException){
              valueFuture.completeExceptionally(ex);
            }else {
              valueFuture.completeExceptionally(new RpcException(RpcException.CLIENT_ERROR, ex));
            }
          }

        }
      });
      return valueFuture;
    } else {
      try {
        RESP response = respFuture.get();
        Object value = null;
        try {
          value = protocol.decodeReturnValue(invocation.getMethod(), response);
        } catch (Exception e) {
          if(e instanceof RpcException){
            throw (RpcException)e;
          }else{
            throw new RpcException(RpcException.SERVER_ERROR, e);
          }
        }
        return value;
      } catch (InterruptedException | ExecutionException e) {
        throw new RpcException(RpcException.CLIENT_ERROR, e);
      }
    }
  }

  private Client newClient(Server server) {
    Client client = new Client(protocol, useSSL, server.getHost(), server.getPort(), clientOptions);
    serverClientMap.put(server, client);
    return client;
  }
}
