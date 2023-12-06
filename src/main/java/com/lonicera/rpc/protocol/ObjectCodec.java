package com.lonicera.rpc.protocol;

import java.lang.reflect.Type;

public interface ObjectCodec {
  Object decodeValue(byte[] bytes, Type type) throws Exception;
  Object[] decodeValues(byte[] bytes, Type[] types) throws Exception;
  byte[] encodeValue(Object value) throws Exception;
  String contentType();
}
