package com.ifs.asdelk;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class ODateEntityMapper {

    public static <E> E readFromClient(ClientEntity clientEntity, Class<E> entityClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, EdmPrimitiveTypeException {
        E object = entityClass.newInstance();

        for(ClientProperty cp : clientEntity.getProperties()){
            String fieldName = cp.getName();
            ClientPrimitiveValue primitiveValue = cp.getPrimitiveValue();
            String setterMethodName = "set".concat(fieldName);

            if(primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Boolean){
                ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Boolean.class, cp.getPrimitiveValue().toCastValue(Boolean.class), object);
            }else if(primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.String){
                ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Boolean.class, cp.getPrimitiveValue().toCastValue(String.class), object);
            } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.DateTimeOffset) {
                ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Boolean.class, cp.getPrimitiveValue().toCastValue(LocalDateTime.class), object);

            }

        }
        return object;
    }

    private static <T> void methodInvoker(Class objectClass, String methodName, Class paramType, Object param, T object) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class[] methodParams = new Class[1];
        methodParams[0] = paramType;
        Method setterMethod = objectClass.getDeclaredMethod(methodName, methodParams);
        setterMethod.invoke(object, param);
    }

}
