package com.lonicera.rpc.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.Getter;

@Getter
public class InvocableMethod {

  private Object service;
  private Method serviceMethod;
  private Method interfaceMethod;

  public InvocableMethod(Object service, Method serviceMethod, Method interfaceMethod) {
    this.service = service;
    if (!serviceMethod.isAccessible()) {
      serviceMethod.setAccessible(true);
    }
    this.serviceMethod = serviceMethod;
    this.interfaceMethod = interfaceMethod;
  }

  public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
    return serviceMethod.invoke(service, args);
  }
}
