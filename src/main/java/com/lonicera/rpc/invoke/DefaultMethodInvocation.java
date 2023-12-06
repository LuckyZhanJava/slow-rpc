package com.lonicera.rpc.invoke;

import com.lonicera.rpc.utils.IdGenerator;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

@Getter
public class DefaultMethodInvocation implements MethodInvocation {

  private static final Map<Method, String> METHOD_ID_MAP = new ConcurrentHashMap<>();
  private String id;
  private Method method;
  private String interfaceName;
  private String methodName;
  private String parameterTypesDesc;
  private Object[] args;

  public DefaultMethodInvocation(Method method, Object[] args) {
    this.method = method;
    this.args = args;
    this.id = String.valueOf(IdGenerator.next());
    this.interfaceName = method.getDeclaringClass().getName();
    this.methodName = method.getName();
    this.parameterTypesDesc = Arrays.toString(method.getGenericParameterTypes());
  }
}
