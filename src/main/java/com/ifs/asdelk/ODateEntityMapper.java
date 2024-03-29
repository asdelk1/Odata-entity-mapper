package com.ifs.asdelk;

import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ODateEntityMapper {

    public static <E> E readFromClient(ClientEntity clientEntity, Class<E> entityClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<E> constructor = entityClass.getConstructor(new Class[]{entityClass});
        E object = constructor.newInstance(new Object[]{});

        for(ClientProperty cp : clientEntity.getProperties()){
            String fieldName = cp.getName();
            ClientPrimitiveValue primitiveValue = cp.getPrimitiveValue();
            String prepending;
            if(primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Boolean){
                prepending = "is";
            }else{
                prepending = "set";
            }
            String setterMethodName = prepending.concat(fieldName);

            if(primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Boolean){
                Class[] methodParams = new Class[1];
                methodParams[0] = Boolean.class;
                Method setterMethod = entityClass.getDeclaredMethod(setterMethodName, methodParams);
                setterMethod.invoke(object, new Boolean(cp.getPrimitiveValue().toString()));
            }else if(primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.String){
                Class[] methodParams = new Class[1];
                methodParams[0] = Boolean.class;
                Method setterMethod = entityClass.getDeclaredMethod(setterMethodName, methodParams);
                setterMethod.invoke(object, cp.getPrimitiveValue().toString());
            }

        }
        return object;
    }

}
