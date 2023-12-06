package com.lonicera.rpc.handler;

import com.lonicera.rpc.invoke.MethodInvocation;

public interface HandlerContext {

  Object handle(MethodInvocation invocation);

}
