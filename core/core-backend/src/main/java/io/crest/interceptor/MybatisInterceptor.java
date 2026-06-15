package io.crest.interceptor;

import io.crest.commons.utils.MybatisInterceptorConfig;
import io.crest.utils.BeanUtils;
import io.crest.utils.IDUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
/**
 * MyBatis 加解密拦截器，按配置在写入前加密、查询后解密字段
 */
public class MybatisInterceptor implements Interceptor {

    private List<MybatisInterceptorConfig> interceptorConfigList;

    private boolean useAssignedKeysWithoutJdbcGeneratedKeys;

    private ConcurrentHashMap<String, Class> classMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Map<String, Map<String, MybatisInterceptorConfig>>> interceptorConfigMap = new ConcurrentHashMap<>();

    /**
     * 拦截 MyBatis 更新和查询调用，执行字段加密或解密处理
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        if (parameter != null && methodName.equals("update")) {
            Object processedParameter = process(parameter);
            invocation.getArgs()[1] = processedParameter;
            invocation.getArgs()[0] = mappedStatementWithAssignedKeys(mappedStatement, processedParameter);
        }
        Object returnValue = invocation.proceed();
        Object result = returnValue;
        if (returnValue instanceof ArrayList<?>) {
            List<Object> list = new ArrayList<>();
            boolean isDecrypted = false;
            for (Object val : (ArrayList<?>) returnValue) {
                Object a = undo(val);
                if (a != val) {
                    isDecrypted = true;
                    list.add(a);
                } else {
                    break;
                }
            }
            if (isDecrypted) {
                result = list;
            }
        } else {
            result = undo(returnValue);
        }
        return result;
    }

    /**
     * 获取当前对象类型对应的字段拦截配置，并缓存解析结果
     */
    private Map<String, Map<String, MybatisInterceptorConfig>> getConfig(Object p) {
        Map<String, Map<String, MybatisInterceptorConfig>> result = new HashMap<>();
        if (p == null) {
            return null;
        }
        String pClassName = p.getClass().getName();
        if (interceptorConfigMap.get(pClassName) != null) {
            return interceptorConfigMap.get(pClassName);
        }
        Map<String, List<MybatisInterceptorConfig>> m = new HashMap<>();
        for (MybatisInterceptorConfig interceptorConfig : interceptorConfigList) {
            String className = interceptorConfig.getModelName();
            String attrName = interceptorConfig.getAttrName();
            if (StringUtils.isNotBlank(className)) {
                Class c = classMap.get(className);
                if (c == null) {
                    try {
                        // nosemgrep: java.lang.security.audit.unsafe-reflection.unsafe-reflection
                        c = Class.forName(className);
                        classMap.put(className, c);
                    } catch (ClassNotFoundException e) {
                        continue;
                    }
                }
                if (c.isInstance(p)) {
                    if (result.get(attrName) == null) {
                        result.put(attrName, new HashMap<>());
                    }
                    if (StringUtils.isNotBlank(interceptorConfig.getInterceptorMethod())) {
                        result.get(attrName).put(Methods.encrypt.name(), interceptorConfig);
                    }
                    if (StringUtils.isNotBlank(interceptorConfig.getInterceptorMethod())) {
                        result.get(attrName).put(Methods.decrypt.name(), interceptorConfig);
                    }
                }
            }
        }
        interceptorConfigMap.put(pClassName, result);
        return result;
    }

    /**
     * 在写入数据库前按配置加密对象字段
     */
    private Object process(Object obj) throws Throwable {
        if (obj instanceof Map) {
            Map paramMap = (Map) obj;
            for (Object key : paramMap.keySet()) {
                if (paramMap.get(key) != null) {
                    paramMap.put(key, process(paramMap.get(key)));
                }
            }
            return paramMap;
        }
        Map<String, Map<String, MybatisInterceptorConfig>> localInterceptorConfigMap = getConfig(obj);
        if (isEmpty(localInterceptorConfigMap)) {
            return obj;
        }
        Object newObject = obj.getClass().newInstance();
        BeanUtils.copyBean(newObject, obj);
        for (String attrName : localInterceptorConfigMap.keySet()) {
            if (isEmpty(localInterceptorConfigMap.get(attrName))) {
                continue;
            }
            MybatisInterceptorConfig interceptorConfig = localInterceptorConfigMap.get(attrName).get(Methods.encrypt.name());
            if (interceptorConfig == null || StringUtils.isBlank(interceptorConfig.getInterceptorClass())
                    || StringUtils.isBlank(interceptorConfig.getInterceptorMethod())) {
                continue;
            }
            Object fieldValue = BeanUtils.getFieldValueByName(interceptorConfig.getAttrName(), newObject);
            if (fieldValue != null) {
                // nosemgrep: java.lang.security.audit.unsafe-reflection.unsafe-reflection
                Class<?> processClazz = Class.forName(interceptorConfig.getInterceptorClass());
                Method method = processClazz.getMethod(interceptorConfig.getInterceptorMethod(), Object.class);
                Object processedValue = method.invoke(null, fieldValue);
                if (processedValue instanceof byte[]) {
                    BeanUtils.setFieldValueByName(newObject, interceptorConfig.getAttrName(), processedValue, byte[].class);
                } else {
                    BeanUtils.setFieldValueByName(newObject, interceptorConfig.getAttrName(), processedValue, fieldValue.getClass());
                }
            }
        }

        return newObject;
    }

    /**
     * OB Oracle JDBC treats preparedStatement(sql, keyColumns) as an extra bind slot.
     * For entities whose Long id is supplied by the application, disable generated-key
     * retrieval and make sure null ids are assigned before MyBatis builds SQL.
     */
    private MappedStatement mappedStatementWithAssignedKeys(MappedStatement mappedStatement, Object parameter) {
        if (!useAssignedKeysWithoutJdbcGeneratedKeys
                || mappedStatement.getSqlCommandType() != SqlCommandType.INSERT
                || !(mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator)) {
            return mappedStatement;
        }
        String[] keyProperties = mappedStatement.getKeyProperties();
        if (keyProperties == null || keyProperties.length == 0) {
            return mappedStatement;
        }
        boolean assigned = false;
        for (String keyProperty : keyProperties) {
            assigned = assignLongKeyIfNeeded(mappedStatement, parameter, keyProperty) || assigned;
        }
        if (!assigned) {
            return mappedStatement;
        }
        return copyWithNoKeyGenerator(mappedStatement);
    }

    private boolean assignLongKeyIfNeeded(MappedStatement mappedStatement, Object parameter, String keyProperty) {
        if (StringUtils.isBlank(keyProperty)) {
            return false;
        }
        MetaObject metaObject = mappedStatement.getConfiguration().newMetaObject(parameter);
        if (!metaObject.hasSetter(keyProperty)) {
            return false;
        }
        Object value = metaObject.hasGetter(keyProperty) ? metaObject.getValue(keyProperty) : null;
        if (value != null) {
            return true;
        }
        Class<?> setterType = metaObject.getSetterType(keyProperty);
        if (setterType == Long.class || setterType == Long.TYPE) {
            metaObject.setValue(keyProperty, IDUtils.snowID());
            return true;
        }
        return false;
    }

    private MappedStatement copyWithNoKeyGenerator(MappedStatement mappedStatement) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                mappedStatement.getConfiguration(),
                mappedStatement.getId(),
                mappedStatement.getSqlSource(),
                mappedStatement.getSqlCommandType());
        builder.resource(mappedStatement.getResource());
        builder.fetchSize(mappedStatement.getFetchSize());
        builder.timeout(mappedStatement.getTimeout());
        builder.statementType(mappedStatement.getStatementType());
        builder.keyGenerator(NoKeyGenerator.INSTANCE);
        builder.databaseId(mappedStatement.getDatabaseId());
        builder.lang(mappedStatement.getLang());
        builder.resultOrdered(mappedStatement.isResultOrdered());
        builder.resultSets(join(mappedStatement.getResultSets()));
        builder.resultMaps(mappedStatement.getResultMaps());
        builder.resultSetType(mappedStatement.getResultSetType());
        builder.flushCacheRequired(mappedStatement.isFlushCacheRequired());
        builder.useCache(mappedStatement.isUseCache());
        builder.cache(mappedStatement.getCache());
        builder.parameterMap(mappedStatement.getParameterMap());
        builder.dirtySelect(mappedStatement.isDirtySelect());
        return builder.build();
    }

    private String join(String[] values) {
        return values == null || values.length == 0 ? null : String.join(",", values);
    }


    /**
     * 在查询返回后按配置解密对象字段
     */
    private Object undo(Object obj) throws Throwable {
        Map<String, Map<String, MybatisInterceptorConfig>> localDecryptConfigMap = getConfig(obj);
        Object result;
        if (isEmpty(localDecryptConfigMap)) {
            return obj;
        }
        result = obj.getClass().newInstance();
        BeanUtils.copyBean(result, obj);
        for (String attrName : localDecryptConfigMap.keySet()) {
            if (isEmpty(localDecryptConfigMap.get(attrName))) {
                continue;
            }
            MybatisInterceptorConfig interceptorConfig = localDecryptConfigMap.get(attrName).get(Methods.decrypt.name());
            if (interceptorConfig == null || StringUtils.isBlank(interceptorConfig.getUndoClass())
                    || StringUtils.isBlank(interceptorConfig.getUndoMethod())) {
                continue;
            }
            Object fieldValue = BeanUtils.getFieldValueByName(interceptorConfig.getAttrName(), result);
            if (fieldValue != null) {
                // nosemgrep: java.lang.security.audit.unsafe-reflection.unsafe-reflection
                Class<?> processClazz = Class.forName(interceptorConfig.getUndoClass());
                Object undoValue;
                if (fieldValue instanceof List) {
                    Method method = processClazz.getMethod(interceptorConfig.getUndoMethod(), List.class, String.class);
                    // List 属性已通过引用更新，无需再次写回字段
                    method.invoke(null, fieldValue, interceptorConfig.getAttrNameForList());
                } else {
                    Method method = processClazz.getMethod(interceptorConfig.getUndoMethod(), Object.class);
                    undoValue = method.invoke(null, fieldValue);
                    if (undoValue instanceof byte[]) {
                        BeanUtils.setFieldValueByName(result, interceptorConfig.getAttrName(), undoValue, byte[].class);
                    } else {
                        BeanUtils.setFieldValueByName(result, interceptorConfig.getAttrName(), undoValue, fieldValue.getClass());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 让 MyBatis 为目标对象创建插件代理
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 接收 MyBatis 插件属性配置
     */
    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * 获取字段拦截配置列表
     */
    public List<MybatisInterceptorConfig> getInterceptorConfigList() {
        return interceptorConfigList;
    }

    /**
     * 设置字段拦截配置列表
     */
    public void setInterceptorConfigList(List<MybatisInterceptorConfig> interceptorConfigList) {
        this.interceptorConfigList = interceptorConfigList;
    }

    public void setUseAssignedKeysWithoutJdbcGeneratedKeys(boolean useAssignedKeysWithoutJdbcGeneratedKeys) {
        this.useAssignedKeysWithoutJdbcGeneratedKeys = useAssignedKeysWithoutJdbcGeneratedKeys;
    }

    /**
     * 字段处理方法类型
     */
    private enum Methods {
        encrypt, decrypt
    }

    /**
     * 判断配置映射是否为空
     */
    private boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

}
