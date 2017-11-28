package top.yuxin.hookdemo.hookutils;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Created by yuxin-hu on 17-11-28.
 */

public class ServicesHook {
    private static final String TAG=ServicesHook.class.getSimpleName();
    private String servicename;
    private String interfacePackageName;
    private boolean isStub;

    /**
     * 初始化
     * @param servicename  服务注册时的名称
     * @param interfacePackageName  服务接口包名
     * @param isStub       是否是远程服务
     */
    public ServicesHook(String servicename, String interfacePackageName,boolean isStub) {
        this.servicename = servicename;
        this.interfacePackageName = interfacePackageName;
        this.isStub=isStub;
    }

    public void hookService(InvocationHandler invocationHandler){
        try {
            Class<?> serviceManagerСlazz   = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerСlazz.getDeclaredMethod("getService", String.class);
            if(!getServiceMethod.isAccessible()){
                getServiceMethod.setAccessible(true);
            }

            //通过ServiceManager里的getService方法,获取到对应服务的IBinder
            IBinder b = (IBinder) getServiceMethod.invoke(null, servicename);

            //设置动态代理,代理IBinder
            IBinderHandler iBinderHandler =new IBinderHandler(b,invocationHandler);
            IBinder binderproxy = (IBinder) Proxy.newProxyInstance(b.getClass().getClassLoader(), b.getClass().getInterfaces(), iBinderHandler);

            Field sCacheField = serviceManagerСlazz.getDeclaredField("sCache");
            if(!sCacheField.isAccessible()){
                sCacheField.setAccessible(true);
            }
            //更改ServiceManger里面存储IBinder的map,替换成代理对象
            HashMap<String, IBinder> sCache = (HashMap<String, IBinder>) sCacheField.get(null);
            sCache.remove(servicename);
            sCache.put(servicename,binderproxy);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * hook IBinder的方法调用
     */
    private class IBinderHandler implements InvocationHandler{
        private IBinder b;
        private InvocationHandler invocationHandler;
        private Class<?> stubClazz;

        public IBinderHandler(IBinder b, InvocationHandler invocationHandler) {
            this.b = b;
            this.invocationHandler = invocationHandler;
            try {
                //如果为远程服务那么他调用的是对应接口的Stub内部类,否则调用的是接口
               stubClazz      = Class.forName(String.format("%s%s",interfacePackageName,isStub?"$Stub":""));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxyObj, Method method, Object[] objects) throws Throwable {
            Log.e(TAG,"methodName:"+method.getName());
            //不管是远程调用还是本地调用,在asInterface将IBinder转换成服务在本地的引用IServiceManger时,
            //都会调用queryLocalInterface查询是否是本地服务 这里直接返回服务在本地的引用的代理对象
            if("queryLocalInterface".equals(method.getName())){
                return (IInterface)Proxy.newProxyInstance(proxyObj.getClass().getClassLoader(),stubClazz.getInterfaces(),new StubHook(b,stubClazz,invocationHandler));
            }
            return method.invoke(b,objects);
        }
    }

    /**
     * 动态代理服务在本地的句柄
     * 处理他的方法调用
     */
    private class StubHook implements InvocationHandler{

        private Object mbase;
        private IBinder b;
        private Class<?> stubClazz;
        private InvocationHandler invocationHandler;

        public StubHook(IBinder b,Class<?> stubClazz, InvocationHandler invocationHandler) {
            this.b = b;
            this.invocationHandler = invocationHandler;
            this.stubClazz=stubClazz;
            try {
                //主动调用stubClazz的asInterface获取到服务在本地代理的原始对象
                Method asInterfaceMethod = stubClazz.getDeclaredMethod("asInterface", IBinder.class);
                if(!asInterfaceMethod.isAccessible()){
                    asInterfaceMethod.setAccessible(true);
                }
                mbase = asInterfaceMethod.invoke(null, b);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxyObj, Method method, Object[] objects) throws Throwable {
            //通过接口回调的方式将服务在本地的引用调用的所有方法,全部抛给接口处理,如果设置了代理接口对象的话
            if(invocationHandler!=null){
                invocationHandler.invoke(mbase,method,objects);
            }
            return method.invoke(mbase,objects);
        }
    }

}
