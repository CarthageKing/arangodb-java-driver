package com.arangodb.http;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.arangodb.impl.BaseDriverInterface;

/**
 * Created by fbartels on 10/27/14.
 */
public class InvocationHandlerImpl  implements InvocationHandler {
  private BaseDriverInterface testImpl;

  public InvocationHandlerImpl(BaseDriverInterface impl) {
    this.testImpl = impl;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable {
    if(Object.class  == method.getDeclaringClass()) {
      String name = method.getName();
      if("equals".equals(name)) {
        return proxy == args[0];
      } else if("hashCode".equals(name)) {
        return System.identityHashCode(proxy);
      } else if("toString".equals(name)) {
        return proxy.getClass().getName() + "@" +
          Integer.toHexString(System.identityHashCode(proxy)) +
          ", with InvocationHandler " + this;
      } else {
        throw new IllegalStateException(String.valueOf(method));
      }
    }
    testImpl.getHttpManager().setCurrentObject(
      new InvocationObject(method, testImpl, args)
    );
    return method.invoke(testImpl, args);
  }
}
