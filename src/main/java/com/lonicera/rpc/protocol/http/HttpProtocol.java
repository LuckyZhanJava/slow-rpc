package com.lonicera.rpc.protocol.http;


import com.lonicera.rpc.exception.RpcException;
import com.lonicera.rpc.invoke.MethodInvocation;
import com.lonicera.rpc.protocol.ObjectCodec;
import com.lonicera.rpc.protocol.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class HttpProtocol implements Protocol<HttpRpcRequest, HttpRpcResponse> {

  private ObjectCodec objectCodec;

  public HttpProtocol(ObjectCodec objectCodec) {
    this.objectCodec = objectCodec;
  }

  public HttpProtocol() {
    this(JacksonObjectMapper.objectMapperCodec());
  }

  @Override
  public String name() {
    return "http";
  }

  @Override
  public HttpRpcRequest encodeInvocation(MethodInvocation invocation)
      throws Exception {
    return new HttpRpcRequest(invocation, objectCodec);
  }


  @Override
  public ChannelHandler[] encodeRequestHandlers() {
    return new ChannelHandler[]{
        new HttpRequestEncoder(),
        new HttpRpcRequestWriteHandler()
    };
  }

  @Override
  public ChannelHandler[] decodeRequestHandlers() {
    return new ChannelHandler[]{
        new HttpRequestDecoder(),
        new HttpRpcRequestReadHandler()
    };
  }

  private static final Object[] EMPTY_PARAMETERS = new Object[0];

  @Override
  public Object[] decodeInvocationArgs(byte[] bytes, Method method) throws Exception {

    Type[] types = method.getGenericParameterTypes();

    if (types.length == 0 && bytes.length == 0) {
      return EMPTY_PARAMETERS;
    }

    if (types.length == 1) {
      Type type = types[0];
      Object arg = objectCodec.decodeValue(bytes, type);
      return new Object[]{arg};
    }
    return objectCodec.decodeValues(bytes, types);
  }

  @Override
  public HttpRpcResponse encodeReturnValue(ByteBufAllocator allocator,
      HttpRpcRequest request,
      Object value, Throwable e) {
    HttpResponseStatus status;
    ByteBuf byteBuf = allocator.buffer();
    if (e != null) {
      if (e instanceof RpcException
          && ((RpcException) e).getErrorCode() == RpcException.SERVICE_NOT_FOUND) {
        status = HttpResponseStatus.NOT_FOUND;
      } else {
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }
      return newRpcResponse(request.correlationId(), status, byteBuf, null, e);
    }

    status = HttpResponseStatus.OK;
    try {
      return newRpcResponse(request.correlationId(), status, byteBuf, value, null);
    } catch (Exception ex) {
      log.error("encode value error ", ex);
      status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      return newRpcResponse(request.correlationId(), status, byteBuf, null, ex);
    }
  }

  @NotNull
  private HttpRpcResponse newRpcResponse(String correlationId,
      HttpResponseStatus status,
      ByteBuf byteBuf,
      Object value,
      Throwable t
  ) {
    if(t != null){
      byteBuf.writeCharSequence(t.getMessage(), Charset.forName("utf-8"));
    }else{
      try{
        byte[] bytes = objectCodec.encodeValue(value);
        byteBuf.writeBytes(bytes);
      }catch (Exception e){
        byteBuf.writeCharSequence(t.getMessage(), Charset.forName("utf-8"));
      }

    }
    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
        byteBuf);
    HttpHeaders headers = httpResponse.headers();
    headers.add(HttpProtocolConstants.CORRELATION_ID, correlationId);
    headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    headers.set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
    return new HttpRpcResponse(httpResponse);
  }

  @Override
  public ChannelHandler[] encodeResponseHandlers() {
    return new ChannelHandler[]{
        new HttpResponseEncoder(),
        new HttpRpcResponseWriteHandler()
    };
  }

  @Override
  public ChannelHandler[] decodeResponseHandlers() {
    return new ChannelHandler[]{
        new HttpResponseDecoder(),
        new HttpRpcResponseReadHandler()
    };
  }

  @Override
  public Object decodeReturnValue(Method method, HttpRpcResponse response) throws Exception {
    DefaultFullHttpResponse resp = response.getHttpResponse();
    HttpResponseStatus status = resp.status();
    Class<?> returnType = method.getReturnType();
    Type actualReturnType =
        returnType.equals(CompletableFuture.class) ? ((ParameterizedType) method
            .getGenericReturnType())
            .getActualTypeArguments()[0] : method.getGenericReturnType();
    if (status == HttpResponseStatus.OK) {
      Object value = objectCodec.decodeValue(response.resultBytes(), actualReturnType);
      return value;
    }
    if (status == HttpResponseStatus.NOT_FOUND) {
      throw new RpcException(RpcException.SERVICE_NOT_FOUND,
          new String(response.resultBytes(), "utf-8"));
    }
    if (status == HttpResponseStatus.INTERNAL_SERVER_ERROR) {
      throw new RpcException(RpcException.SERVICE_NOT_FOUND,
          new String(response.resultBytes(), "utf-8"));
    }
    throw new RpcException(RpcException.SERVER_ERROR,
        new String(response.resultBytes(), "utf-8"));
  }

}
