package com.lonicera.rpc.server;

import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.protocol.Protocol;
import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.protocol.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RpcMethodInvokeHandler<REQ extends RpcRequest, RESP extends RpcResponse> extends
    ChannelDuplexHandler {

  private Protocol<REQ, RESP> protocol;
  private ServiceRegistry serviceRegistry;
  private ExecutorService executorService;

  public RpcMethodInvokeHandler(Protocol<REQ, RESP> protocol, ServiceRegistry serviceRegistry,
      ExecutorService executorService) {
    this.protocol = protocol;
    this.serviceRegistry = serviceRegistry;
    this.executorService = executorService;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof RpcRequest) {
      RpcRequest request = (RpcRequest) msg;
      String serviceName = request.serviceName();
      String methodName = request.methodName();
      String parameterTypesDesc = request.parameterTypesDesc();
      InvocableMethod invocableMethod = serviceRegistry
          .getServiceMethod(serviceName, methodName, parameterTypesDesc);

      if (invocableMethod == null) {
        RpcException e = new RpcException(RpcException.SERVICE_NOT_FOUND,
            "service method : " + serviceName + "." + methodName + "#" + parameterTypesDesc);
        RESP resp = protocol
            .encodeReturnValue(ctx.alloc(), (REQ) request, null, e);
        ctx.writeAndFlush(resp);
        return;
      }

      byte[] bytes = ((RpcRequest) msg).argsBytes();
      Object[] args;
      try {
        args = protocol
            .decodeInvocationArgs(bytes, invocableMethod.getInterfaceMethod());
      } catch (Throwable e) {
        log.error("decode method args error", e);
        RESP resp = protocol
            .encodeReturnValue(ctx.alloc(), (REQ) request, null, e);
        ctx.writeAndFlush(resp);
        return;
      }

      Runnable invokeTask = newInvokeMethodTask((REQ) request, invocableMethod, args,
          ctx.channel());
      executorService.submit(invokeTask);

    } else {
      super.channelRead(ctx, msg);
    }
  }

  private Runnable newInvokeMethodTask(
      REQ request,
      InvocableMethod invocableMethod,
      Object[] args,
      Channel channel
  ) {
    return new Runnable() {
      @Override
      public void run() {
        Object value = null;
        Throwable t = null;
        try {
          RpcContext.getContext().attachments(request.getAttachments());
          value = invocableMethod.invoke(args);
        } catch (InvocationTargetException e) {
          t = e.getCause();
        } catch (IllegalAccessException e) {
          t = e;
        } finally {
          RpcContext.clearContext();
        }

        if (value instanceof CompletableFuture) {
          ((CompletableFuture) value).whenComplete((v, e) -> {
            RESP resp = protocol
                .encodeReturnValue(channel.alloc(), (REQ) request, v, (Throwable) e);
            channel.writeAndFlush(resp);
          });
        } else {
          RESP resp = protocol
              .encodeReturnValue(channel.alloc(), (REQ) request, value, (Throwable) t);
          channel.writeAndFlush(resp);
        }
      }
    };
  }
}
