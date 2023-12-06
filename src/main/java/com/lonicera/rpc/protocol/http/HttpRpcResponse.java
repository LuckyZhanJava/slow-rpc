package com.lonicera.rpc.protocol.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lonicera.rpc.protocol.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

public class HttpRpcResponse implements RpcResponse {

  private DefaultFullHttpResponse httpResponse;
  private String correlationId;
  private byte[] resultBytes;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public HttpRpcResponse(DefaultFullHttpResponse httpResponse){
    this.httpResponse = httpResponse;
    String correlationId = this.httpResponse.headers().get(HttpProtocolConstants.CORRELATION_ID);
    if(correlationId == null){
      throw new IllegalArgumentException("httpResponse headers not contain correlation id");
    }
    this.correlationId = correlationId;
    ByteBuf buf = httpResponse.content();
    resultBytes = ByteBufUtil.getBytes(buf);
  }

  @Override
  public String correlationId() {
    return correlationId;
  }

  @Override
  public byte code() {
    return (byte) httpResponse.status().code();
  }

  @Override
  public byte[] resultBytes() {
    return resultBytes;
  }

  public DefaultFullHttpResponse getHttpResponse() {
    return httpResponse;
  }
}
