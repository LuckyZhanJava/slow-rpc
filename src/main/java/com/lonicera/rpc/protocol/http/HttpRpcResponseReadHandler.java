package com.lonicera.rpc.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import java.util.List;

class HttpRpcResponseReadHandler extends MessageToMessageDecoder<HttpObject> {

  private HttpObject last;

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
      throws Exception {
    if(last == null){
      last = msg;
    }else{
      if(last instanceof HttpResponse && msg instanceof HttpContent){
        HttpResponse response = (HttpResponse) last;
        DefaultFullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(response.protocolVersion(), response.status(), ((HttpContent) msg).content());
        fullHttpResponse.headers().setAll(response.headers());
        out.add(new HttpRpcResponse(fullHttpResponse));
      }else{
        last = null;
      }
    }
  }
}
