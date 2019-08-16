package com.ifs.asdelk;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.domain.*;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

public class ODataEntityMapper {

    public static <E> E readFromClient(ClientEntity clientEntity, Class<E> entityClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, EdmPrimitiveTypeException, NoSuchFieldException {
        E object = entityClass.newInstance();

        for (ClientProperty cp : clientEntity.getProperties()) {
            ODataEntityMapper.getValue(cp, entityClass, object);
        }
        return object;
    }

    public static <E> void writeToClient(ODataClient client, EdmEntityType edmEntityType, ClientEntity ce, E entity, boolean skipKeys) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> entityClass = entity.getClass();
        ClientObjectFactory clientObjectFactory = client.getObjectFactory();
        for (String propertyName : edmEntityType.getPropertyNames()) {
//            Need to keep this until the implementation of the fields validation with EDM
//            Field field = entityClass.getField(propertyName.substring(0, 1).toLowerCase().concat(propertyName.substring(1)));

            if (skipKeys) {
                List<EdmKeyPropertyRef> keys = edmEntityType.getKeyPropertyRefs();
                if (keys.stream().anyMatch((EdmKeyPropertyRef key) -> key.getName().equals(propertyName))) {
                    continue;
                }
            }

            Method getterMethod = entityClass.getDeclaredMethod("get".concat(propertyName));
            Object fieldValue = getterMethod.invoke(entity);
            if (fieldValue != null) {
                ClientProperty cp = null;
                if (fieldValue.getClass() == String.class) {
                    cp = clientObjectFactory.newPrimitiveProperty(propertyName,
                            clientObjectFactory.newPrimitiveValueBuilder().buildString((String) fieldValue));
                } else if (fieldValue.getClass() == Integer.class) {
                    cp = clientObjectFactory.newPrimitiveProperty(propertyName,
                            clientObjectFactory.newPrimitiveValueBuilder().buildInt32((Integer) fieldValue));
                } else if (fieldValue.getClass() == Double.class)
                    cp = clientObjectFactory.newPrimitiveProperty(propertyName,
                            clientObjectFactory.newPrimitiveValueBuilder().buildDouble((Double) fieldValue));
                else if (fieldValue.getClass() == LocalDateTime.class) {
                    LocalDateTime dateTimeValue = (LocalDateTime) fieldValue;
                    cp = clientObjectFactory.newPrimitiveProperty(propertyName,
                            clientObjectFactory.newPrimitiveValueBuilder().buildString(dateTimeValue.toString()));
                }
                if (cp != null) {
                    ce.getProperties().add(cp);
                }
            }
        }
    }

    private static <T> void methodInvoker(Class objectClass, String methodName, Class paramType, Object param, T object) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (object != null) {
            Class[] methodParams = new Class[1];
            methodParams[0] = paramType;
            Method setterMethod = objectClass.getDeclaredMethod(methodName, methodParams);
            setterMethod.invoke(object, param);
        }
    }

    private static <T> void getValue(ClientProperty clientProperty, Class<T> entityClass,Object instance) throws EdmPrimitiveTypeException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, InstantiationException {
        String fieldName = clientProperty.getName();
        String setterMethodName = "set".concat(fieldName);

        if (clientProperty.hasPrimitiveValue()) {
            ClientPrimitiveValue primitiveValue = clientProperty.getPrimitiveValue();

            //TODO Add Validation for column type mismatch

            if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Boolean) {
                ODataEntityMapper.methodInvoker(entityClass, setterMethodName, Boolean.class, clientProperty.getPrimitiveValue().toCastValue(Boolean.class), instance);
            } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.String) {
                ODataEntityMapper.methodInvoker(entityClass, setterMethodName, String.class, clientProperty.getPrimitiveValue().toCastValue(String.class), instance);
            } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.DateTimeOffset) {
                ODataEntityMapper.methodInvoker(entityClass, setterMethodName, LocalDateTime.class, clientProperty.getPrimitiveValue().toCastValue(LocalDateTime.class), instance);
            } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int16 || primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int32) {
                ODataEntityMapper.methodInvoker(entityClass, setterMethodName, Integer.class, clientProperty.getPrimitiveValue().toCastValue(Integer.class), instance);
            } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int64 || primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Decimal ||
                    primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Double) {
                ODataEntityMapper.methodInvoker(entityClass, setterMethodName, Double.class, clientProperty.getPrimitiveValue().toCastValue(Double.class), instance);
            }
        } else if (clientProperty.hasEnumValue()) {
            ODataEntityMapper.methodInvoker(entityClass, setterMethodName, String.class, clientProperty.getEnumValue().getValue(), instance);
        }else if(clientProperty.hasComplexValue()){
            ClientComplexValue complexValue = clientProperty.getComplexValue();
            Field complexField = entityClass.getDeclaredField(ODataEntityMapper.getLowerCase(fieldName));
            Class<?> complexClassType =  complexField.getDeclaringClass();
            Object complexInstance =  complexClassType.newInstance();
            Iterator<ClientProperty> propertyIterator = complexValue.iterator();
           while(propertyIterator.hasNext()){
               ClientProperty cp = propertyIterator.next();
               ODataEntityMapper.getValue(cp, complexClassType, complexInstance);
            }
            ODataEntityMapper.methodInvoker(entityClass, setterMethodName, complexClassType, complexInstance, instance);
        }
    }

    private static String getLowerCase(String source){
        return source.substring(0,1).toLowerCase().concat(source.substring(1));
    }
}