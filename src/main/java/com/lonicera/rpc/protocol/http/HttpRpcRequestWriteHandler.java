package com.lonicera.rpc.protocol.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;

class HttpRpcRequestWriteHandler extends ChannelDuplexHandler {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof HttpRpcRequest) {
      ctx.write(((HttpRpcRequest) msg).getHttpRequest(), promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
