package com.lonicera.rpc.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import java.util.List;

class HttpRpcRequestReadHandler extends MessageToMessageDecoder<HttpObject> {

  private HttpObject last;

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
      throws Exception {
    if(last == null && msg instanceof DefaultHttpRequest){
        last = msg;
    }else{
      if(msg instanceof HttpContent){
        DefaultHttpRequest request = (DefaultHttpRequest) last;
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(request.protocolVersion(),
            request.method(), request.uri(), ((HttpContent) msg).content().retain());
        out.add(new HttpRpcRequest(httpRequest));
        last = null;
      }
    }
  }
}
