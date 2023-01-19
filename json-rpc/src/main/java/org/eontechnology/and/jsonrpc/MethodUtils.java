package org.eontechnology.and.jsonrpc;

import java.lang.reflect.Method;
import java.util.Locale;

class MethodUtils {

  static Method getMethod(Object obj, String methodName) {

    Method[] ms = obj.getClass().getMethods();

    for (Method m : ms) {
      if (m.getName().equals(methodName)) {
        return m;
      }
    }

    if (methodName.contains("_")) {

      String[] nameSet = methodName.split("_");
      StringBuilder name = new StringBuilder(nameSet[0]);
      for (int i = 1; i < nameSet.length; i++) {
        String item = nameSet[i];
        if (item.length() > 0) {
          name.append(item.substring(0, 1).toUpperCase(Locale.ENGLISH));
          name.append(item.substring(1));
        }
      }

      return getMethod(obj, name.toString());
    }

    return null;
  }
}
