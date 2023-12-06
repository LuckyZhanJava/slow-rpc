package com.lonicera.rpc.protocol.http;

import com.lonicera.rpc.invoke.MethodInvocation;
import com.lonicera.rpc.protocol.ObjectCodec;
import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.server.RpcContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HttpRpcRequest implements RpcRequest {

  private DefaultFullHttpRequest httpRequest;
  private String correlationId;
  private String serviceName;
  private String methodName;
  private String parameterTypesDesc;
  private Map<String,Object> attachments;

  public HttpRpcRequest(
      MethodInvocation invocation,
      ObjectCodec objectCodec
  ) throws Exception {
    this.serviceName = invocation.getInterfaceName();
    this.methodName = invocation.getMethodName();
    this.parameterTypesDesc = invocation.getParameterTypesDesc();
    this.correlationId = invocation.getId();
    this.attachments = RpcContext.getContext().attachments();
    QueryStringEncoder encoder = new QueryStringEncoder("/");
    encoder.addParam(HttpProtocolConstants.SERVICE_NAME, serviceName);
    encoder.addParam(HttpProtocolConstants.METHOD_NAME, methodName);
    encoder.addParam(HttpProtocolConstants.PARAMETER_TYPES_DESC, parameterTypesDesc);
    encoder.addParam(HttpProtocolConstants.CORRELATION_ID, correlationId);
    httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, encoder.toString());
    attach(httpRequest, attachments);
    Object[] args = invocation.getArgs();
    if(args.length == 0){
      httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    }else {
      byte[] bytes;
      if(args.length == 1){
        bytes = objectCodec.encodeValue(invocation.getArgs()[0]);
      }else{
        bytes = objectCodec.encodeValue(invocation.getArgs());
      }
      httpRequest.content().writeBytes(bytes);
      httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
      httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, objectCodec.contentType());
    }
  }

  private void attach(DefaultFullHttpRequest httpRequest, Map<String, Object> attachments) {
    HttpHeaders headers = httpRequest.headers();
    for(Entry<String,Object> attachment : attachments.entrySet()){
      headers.add(attachment.getKey(), attachment.getValue());
    }
  }

  public HttpRpcRequest(DefaultFullHttpRequest httpRequest) {
    this.httpRequest = httpRequest;
    QueryStringDecoder decoder = new QueryStringDecoder(httpRequest.uri());
    Map<String, List<String>> parameterMap = decoder.parameters();
    List<String> serviceNameList = parameterMap.get(HttpProtocolConstants.SERVICE_NAME);
    this.serviceName = headerValue(serviceNameList);
    List<String> methodNameList = parameterMap.get(HttpProtocolConstants.METHOD_NAME);
    this.methodName = headerValue(methodNameList);
    List<String> parameterTypeNamesList = parameterMap.get(HttpProtocolConstants.PARAMETER_TYPES_DESC);
    this.parameterTypesDesc = headerValue(parameterTypeNamesList);
    List<String> correlationIdList = parameterMap.get(HttpProtocolConstants.CORRELATION_ID);
    this.correlationId = headerValue(correlationIdList);
    this.attachments = attachment(httpRequest);
  }

  private Map<String, Object> attachment(DefaultFullHttpRequest httpRequest) {
    HttpHeaders headers = httpRequest.headers();
    Map<String,Object> attchments = new HashMap<>();
    for(Entry<String,String> entry : headers){
      String key = entry.getKey();
      if(!key.equals("content-type") && !key.equals("content-length")){
        attchments.put(entry.getKey(), entry.getValue());
      }
    }
    return attchments;
  }

  private String headerValue(List<String> correlationIdList) {
    if(correlationIdList.size() == 0){
      return null;
    }
    if(correlationIdList.size() == 1){
      return correlationIdList.get(0);
    }
    if(correlationIdList.size() > 1){
      return correlationIdList.stream().collect(Collectors.joining(","));
    }
    throw new Error();
  }

  @Override
  public String correlationId() {
    return correlationId;
  }

  @Override
  public String serviceName() {
    return serviceName;
  }

  @Override
  public String methodName() {
    return methodName;
  }

  @Override
  public String parameterTypesDesc() {
    return parameterTypesDesc;
  }

  @Override
  public Map<String, Object> getAttachments() {
    return attachments;
  }

  @Override
  public byte[] argsBytes() {
    return ByteBufUtil.getBytes(httpRequest.content());
  }

  public DefaultFullHttpRequest getHttpRequest() {
    return httpRequest;
  }

  @Override
  public String toString() {
    return httpRequest.toString();
  }
}
