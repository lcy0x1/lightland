package com.lcy0x1.base.proxy;

import com.hikarishima.lightland.util.LightLandStringUtils;
import com.lcy0x1.base.proxy.annotation.ForEachProxy;
import com.lcy0x1.base.proxy.annotation.ForFirstProxy;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public interface ProxyContainer<T extends ProxyMethod> {
    String[] errMsgSearchList = {"%M", "%B", "%A"};

    static ProxyContainerHandlerCache.OnProxy onForeachProxy(Object obj, ForEachProxy forEachProxy) {
        if (!(obj instanceof ProxyContainer<?>)) return ProxyContainerHandlerCache.callSuper;
        Class<?>[] type = forEachProxy.value();
        Collection<Class<?>> classes;
        switch (type.length) {
            case 0:
                classes = null;
                break;
            case 1:
                classes = Collections.singletonList(type[0]);
                break;
            default:
                classes = new HashSet<>(Arrays.asList(type));
        }
        return onForeachProxy(classes);
    }

    @Nullable
    static ProxyContainerHandlerCache.OnProxy onForeachProxy(Collection<Class<?>> classes) {
        return (o, m, a, proxy1) -> {
            if (!(o instanceof ProxyContainer<?>)) return Proxy.Result.failed;
            ((ProxyContainer<?>) o).getProxy().forEachProxy(p -> {
                if (classes == null || classes.stream().allMatch(c -> c.isInstance(p))) {
                    p.onProxy(o, m, a, proxy1);
                }
            });
            return Proxy.failed();
        };
    }

    static ProxyContainerHandlerCache.OnProxy onForFirstProxy(ForFirstProxy forFirstProxy) {
        final Collection<Class<?>> classes;
        switch (forFirstProxy.value().length) {
            case 0:
                classes = null;
                break;
            case 1:
                classes = Collections.singletonList(forFirstProxy.value()[0]);
                break;
            default:
                classes = new HashSet<>(Arrays.asList(forFirstProxy.value()));
        }
        return (o, m, a, p) -> onForFirstProxy(o, m, a, p, forFirstProxy, classes);
    }

    @NotNull
    Proxy<? extends T> getProxy() throws Throwable;

    /**
     * will be call when proxy method invoke.
     * 在代理方法被调用时，该方法会被调用
     */
    default Proxy.Result<?> onProxy(Method method, Object[] args, MethodProxy proxy) throws Throwable {
        ProxyContainerHandlerCache.OnProxy handler = ProxyContainerHandlerCache.INSTANCE.getHandler(method);
        if (handler != null) {
            return handler.onProxy(this, method, args, proxy);
        }

        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof ForEachProxy) {
                ForEachProxy forEachProxy = (ForEachProxy) annotation;
                handler = onForeachProxy(this, forEachProxy);
                break;
            } else if (annotation instanceof ForFirstProxy) {
                final ForFirstProxy forFirstProxy = (ForFirstProxy) annotation;
                handler = onForFirstProxy(forFirstProxy);
                break;
            }
        }

        if (handler == null) {
            handler = ProxyContainerHandlerCache.callSuper;
        }
        ProxyContainerHandlerCache.INSTANCE.setHandler(method, handler);

        return handler.onProxy(this, method, args, proxy);
    }

    static Proxy.Result<?> onForFirstProxy(Object obj, Method method, Object[] args, MethodProxy proxy, ForFirstProxy forFirstProxy, Collection<Class<?>> classes) throws Throwable {
        if (!(obj instanceof ProxyContainer<?>)) return Proxy.failed();
        final ProxyContainer<?> block = (ProxyContainer<?>) obj;
        final Proxy.Result<?> result = block.getProxy().forFirstProxy(p -> {
            if (classes == null || classes.stream().anyMatch(c -> c.isInstance(p))) {
                return p.onProxy(obj, method, args, proxy);
            } else {
                return Proxy.failed();
            }
        });

        if (result != null && result.isSuccess()) {
            return result;
        }

        // when request not handled
        if (forFirstProxy.must()) {
            // generate error message
            String errMsg = forFirstProxy.errMsg();
            if (StringUtils.isBlank(errMsg)) {
                errMsg = "no proxy handled on method %M";
            }

            final String[] replacementList = new String[errMsgSearchList.length];
            final boolean[] contains = LightLandStringUtils.contains(errMsg, errMsgSearchList);
            if (contains[0]) {
                replacementList[0] = method.toString();
            }
            if (contains[1]) {
                replacementList[1] = block.toString();
            }
            if (contains[2]) {
                replacementList[2] = Arrays.toString(args);
            }

            errMsg = StringUtils.replaceEach(errMsg, errMsgSearchList, replacementList);
            throw forFirstProxy.errClass().getConstructor(String.class).newInstance(errMsg);
        }
        return Proxy.Result.failed;
    }
}
