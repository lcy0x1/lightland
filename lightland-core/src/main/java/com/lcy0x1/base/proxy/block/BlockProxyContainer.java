package com.lcy0x1.base.proxy.block;

import com.lcy0x1.base.proxy.block.annotation.ForEachProxy;
import com.lcy0x1.base.proxy.block.annotation.ForFirstProxy;
import com.lcy0x1.base.proxy.block.handler.IState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface BlockProxyContainer<T> {
    Predicate<Object> stateContainerBuilderFilter = o -> o instanceof StateContainer.Builder;
    String[] errMsgSearchList = {"%M", "%B", "%A"};

    @NotNull
    BlockProxy<T> getProxy();

    /**
     * will be call when proxy method invoke.
     */
    default BlockProxy.Result<?> onProxy(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        HandlerCache.OnProxy handler = HandlerCache.INSTANCE.getHandler(method);
        if (handler != null) {
            return handler.onProxy(obj, method, args, proxy);
        }

        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof ForEachProxy) {
                ForEachProxy forEachProxy = (ForEachProxy) annotation;
                handler = onForeachProxy(obj, method, args, proxy, forEachProxy);
                break;
            } else if (annotation instanceof ForFirstProxy) {
                final ForFirstProxy forFirstProxy = (ForFirstProxy) annotation;
                handler = (obj1, method1, args1, proxy1) -> {
                    final BlockProxy.Result<?> result = onForFirstProxy(obj1, method1, args1, proxy1, forFirstProxy);
                    if (result != null && result.success) {
                        return result;
                    } else {
                        return BlockProxy.of(proxy1.invokeSuper(obj1, args1));
                    }
                };
            }
        }
        if (handler == null) {
            handler = HandlerCache.callSuper;
        }

        HandlerCache.INSTANCE.setHandler(method, handler);

        return handler.onProxy(obj, method, args, proxy);
    }

    default BlockProxy.Result<?> onForFirstProxy(Object obj, Method method, Object[] args, MethodProxy proxy, ForFirstProxy forFirstProxy) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!(obj instanceof Block)) return BlockProxy.Result.failed;
        final Block block = (Block) obj;

        // todo impl official interfaces
        // just return when call handled

        // when request not handled
        if (forFirstProxy.must()) {
            // generate error message
            String errMsg = forFirstProxy.errMsg();
            if (StringUtils.isBlank(errMsg)) {
                errMsg = "no proxy handled on method %M";
            }

            final String[] replacementList = new String[errMsgSearchList.length];
            // todo use efficient contains
            if (errMsg.contains(errMsgSearchList[0])) {
                replacementList[0] = method.toString();
            }
            if (errMsg.contains(errMsgSearchList[1])) {
                replacementList[1] = block.toString();
            }
            if (errMsg.contains(errMsgSearchList[2])) {
                replacementList[2] = Arrays.toString(args);
            }

            errMsg = StringUtils.replaceEach(errMsg, errMsgSearchList, replacementList);
            throw forFirstProxy.errClass().getConstructor(String.class).newInstance(errMsg);
        }
        return BlockProxy.Result.failed;
    }

    default HandlerCache.OnProxy onForeachProxy(Object obj, Method method, Object[] args, MethodProxy proxy, ForEachProxy forEachProxy) {
        if (!(obj instanceof Block)) return HandlerCache.callSuper;
        Class<?>[] type = forEachProxy.type();
        Collection<Class<?>> classes;
        switch (type.length) {
            case 0:
                return HandlerCache.callSuper;
            case 1:
                classes = Collections.singletonList(type[0]);
                break;
            default:
                classes = new HashSet<>(Arrays.asList(type));
        }
        return onForeachProxy((Block) obj, method, args, proxy, forEachProxy, classes);
    }

    @Nullable
    default HandlerCache.OnProxy onForeachProxy(Block block, Method method, Object[] args, MethodProxy proxy, ForEachProxy forEachProxy, Collection<Class<?>> classes) {
        if (classes.contains(IState.class)) {
            return (obj, method1, args1, proxy1) -> {
                if (!(obj instanceof Block)) return BlockProxy.Result.failed;
                getProxy().forEachProxy(p -> {
                    if (p instanceof IState) {
                        Stream<Object> stream = Arrays.stream(args1);
                        final Optional<Object> stateContainerBuilder = stream.filter(stateContainerBuilderFilter).findFirst();
                        //noinspection unchecked
                        stateContainerBuilder.ifPresent(o -> ((IState) p).createBlockStateDefinition((Block) obj,
                                (StateContainer.Builder<Block, BlockState>) o));
                    }
                });
                return BlockProxy.of(null);
            };
        }
        // todo impl other official interfaces
        return null;
    }
}
