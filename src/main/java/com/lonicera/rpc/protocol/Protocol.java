package com.lonicera.rpc.protocol;

import com.lonicera.rpc.invoke.MethodInvocation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public interface Protocol<REQ extends RpcRequest, RESP extends RpcResponse> {

  String name();

  REQ encodeInvocation(MethodInvocation invocation) throws Exception;

  ChannelHandler[] encodeRequestHandlers();

  ChannelHandler[] decodeRequestHandlers();

  Object[] decodeInvocationArgs(byte[] bytes, Method method)
      throws Exception;

  RESP encodeReturnValue(ByteBufAllocator allocator, REQ request, Object value, Throwable e);

  ChannelHandler[] encodeResponseHandlers();

  ChannelHandler[] decodeResponseHandlers();

  Object decodeReturnValue(Method method, RESP resp) throws Exception;
}
