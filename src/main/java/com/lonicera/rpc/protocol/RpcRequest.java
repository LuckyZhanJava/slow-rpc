package com.lonicera.rpc.protocol;

import java.util.Map;

public interface RpcRequest {
  String correlationId();
  String serviceName();
  String methodName();
  String parameterTypesDesc();
  Map<String,Object> getAttachments();
  byte[] argsBytes();
}
