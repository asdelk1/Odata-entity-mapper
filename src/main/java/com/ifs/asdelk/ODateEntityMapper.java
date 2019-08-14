package com.ifs.asdelk;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

public class ODateEntityMapper {

    public static <E> E readFromClient(ClientEntity clientEntity, Class<E> entityClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, EdmPrimitiveTypeException {
        E object = entityClass.newInstance();

        for (ClientProperty cp : clientEntity.getProperties()) {
            String fieldName = cp.getName();
            String setterMethodName = "set".concat(fieldName);
            if (cp.hasPrimitiveValue()) {
                ClientPrimitiveValue primitiveValue = cp.getPrimitiveValue();

                //TODO Add Validation for column type mismatch

                if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Boolean) {
                    ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Boolean.class, cp.getPrimitiveValue().toCastValue(Boolean.class), object);
                } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.String) {
                    ODateEntityMapper.methodInvoker(entityClass, setterMethodName, String.class, cp.getPrimitiveValue().toCastValue(String.class), object);
                } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.DateTimeOffset) {
                    ODateEntityMapper.methodInvoker(entityClass, setterMethodName, LocalDateTime.class, cp.getPrimitiveValue().toCastValue(LocalDateTime.class), object);
                } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int16 || primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int32) {
                    ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Integer.class, cp.getPrimitiveValue().toCastValue(Integer.class), object);
                } else if (primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Int64 || primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Decimal ||
                        primitiveValue.getTypeKind() == EdmPrimitiveTypeKind.Double) {
                    ODateEntityMapper.methodInvoker(entityClass, setterMethodName, Double.class, cp.getPrimitiveValue().toCastValue(Double.class), object);
                }
            } else if (cp.hasEnumValue()) {
                ODateEntityMapper.methodInvoker(entityClass, setterMethodName, String.class, cp.getEnumValue().getValue(), object);
            }

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


}
