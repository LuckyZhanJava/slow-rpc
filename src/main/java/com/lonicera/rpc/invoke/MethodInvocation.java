package com.lonicera.rpc.invoke;

import java.lang.reflect.Method;

public interface MethodInvocation {
  String getId();
  Method getMethod();
  String getInterfaceName();
  String getMethodName();
  String getParameterTypesDesc();
  Object[] getArgs();
}
