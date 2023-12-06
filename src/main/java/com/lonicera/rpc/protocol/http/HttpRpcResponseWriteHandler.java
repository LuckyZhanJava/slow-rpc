package com.lonicera.rpc.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

class HttpRpcResponseWriteHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if(msg instanceof HttpRpcResponse){
      ctx.writeAndFlush(((HttpRpcResponse) msg).getHttpResponse());
    }else{
      ctx.writeAndFlush(msg);
    }
  }
}
