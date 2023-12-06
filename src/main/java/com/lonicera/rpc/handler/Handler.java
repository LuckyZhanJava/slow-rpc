package com.lonicera.rpc.handler;


import com.lonicera.rpc.client.RequestOptions;
import com.lonicera.rpc.invoke.MethodInvocation;

public interface Handler {

  Object handle(HandlerContext context, Server server, RequestOptions options, MethodInvocation invocation);

}
