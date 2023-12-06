package com.lonicera.rpc.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.java.Log;

@Log
class ServiceRegistry {

  private Map<String, Map<String, InvocableMethod>> serviceMethodMap = new ConcurrentHashMap<>();
  private List<Object> registerServiceList = new ArrayList<>();

  public ServiceRegistry() {

  }

  public void register(Object service) {
    Class<?> clazz = service.getClass();
    Class<?>[] interfaces = clazz.getInterfaces();
    if (interfaces.length == 0) {
      throw new IllegalArgumentException("service class has none interface");
    }
    if (interfaces.length != 1) {
      log.warning("service has multi interface, only the first will be register");
    }
    Class<?> interfaceClazz = interfaces[0];
    registerMethod(service, interfaceClazz, clazz);
    registerServiceList.add(service);
  }

  private void registerMethod(Object service, Class<?> interfaceClass, Class<?> serviceClass) {
    Method[] interfaceMethods = interfaceClass.getMethods();
    Map<String, InvocableMethod> methodIdInvocableMap = new HashMap<>();
    for (Method interfaceMethod : interfaceMethods) {
      Method serviceMethod;
      try {
        serviceMethod = serviceClass
            .getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException(e);
      }
      InvocableMethod invocableMethod = new InvocableMethod(service, serviceMethod,
          interfaceMethod);
      String methodId = interfaceMethod.getName() + "#" + Arrays
          .toString(interfaceMethod.getGenericParameterTypes());
      methodIdInvocableMap.put(methodId, invocableMethod);
    }
    serviceMethodMap.put(interfaceClass.getName(), methodIdInvocableMap);
  }

  public InvocableMethod getServiceMethod(String serviceName, String methodName,
      String parameterTypesDesc) {
    Map<String, InvocableMethod> methodDescKeyMap = serviceMethodMap.get(serviceName);
    if (methodDescKeyMap == null) {
      return null;
    }
    return methodDescKeyMap.get(methodName + "#" + parameterTypesDesc);
  }

  public List<Object> getServices() {
    return new ArrayList<>(registerServiceList);
  }
}
