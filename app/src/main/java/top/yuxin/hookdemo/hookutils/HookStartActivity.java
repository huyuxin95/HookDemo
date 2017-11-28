package top.yuxin.hookdemo.hookutils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import android.os.*;

import top.yuxin.hookdemo.ProxyActivity;

/**
 * Created by yuxin-hu on 17-11-27.
 */

public class HookStartActivity {

    private Context context;
    private static final  String ORGIN_INTENT="orgin_intent";


    public HookStartActivity(Context context) {
        this.context = context;
    }

    /**
     *  第一步更改传入信息
     */
    public void hookAms(){
        try {
            //Instrumentation的execStartActivity中
            final Class<?> amnClazz = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = amnClazz.getDeclaredField("gDefault");  //获取IActivityManager的单例封装
            gDefaultField.setAccessible(true);
            Object gDefault = gDefaultField.get(null);

            Class<?> singletonClazz = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClazz.getDeclaredField("mInstance");//从封装的单例中获取IActivityManager
            mInstanceField.setAccessible(true);
            final Object orginObj = mInstanceField.get(gDefault);

            //动态代理IActivityManager,他是AMS在用户端的引用
            //当其调用startActivity时替换intent信息
            Object proxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{Class.forName("android.app.IActivityManager")}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxyObj, Method method, Object[] objects) throws Throwable {
                    Log.e("TAG","MethodName:"+method.getName());
                    if("startActivity".equals(method.getName())){
                        Intent orginIntent=null;
                        int index=-1;
                        for (int i = 0; i < objects.length; i++) {
                            if(objects[i] instanceof Intent){
                                orginIntent= (Intent) objects[i];
                                index=i;
                            }
                        }
                        //更换为已注册的Activity,骗过检查
                        ComponentName componentName=new ComponentName(context,ProxyActivity.class);
                        Intent proxyIntent=new Intent();
                        proxyIntent.setComponent(componentName);
                        proxyIntent.putExtra(ORGIN_INTENT,orginIntent);
                        objects[index]=proxyIntent;
                    }
                    return method.invoke(orginObj,objects);
                }
            });
            mInstanceField.set(gDefault,proxyInstance);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更改传出信息
     */
    private void hookActivityThread(){
        try {
            //AMS具有ApplicationThread的引用,通过这个引用控制Activity的生命周期,ApplicationThread是ActivityThread内部类
            Class<?> activityClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object activtyThread = currentActivityThreadMethod.invoke(null);

            //当ApplicationThread被调用scheduleLaunchActivity时,会通过Handler将消息发送出来
            Field mHField = activityClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler handler = (Handler) mHField.get(activtyThread);

            //通过静态代理,替换Handler中的mCallback对象
            Class<?> handleClazz = Class.forName("android.os.Handler");
            Field mCallbackField = handleClazz.getDeclaredField("mCallback");
            if(!mCallbackField.isAccessible()){
                mCallbackField.setAccessible(true);
            }
            Handler.Callback mCallback = (Handler.Callback) mCallbackField.get(handler);
            CallBackProxy callBackProxy = new CallBackProxy(handler);
            mCallbackField.set(handler,callBackProxy);


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

    private class CallBackProxy implements Handler.Callback{
        private  Handler handler;

        public CallBackProxy(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message message) {
            //what==100 时 LAUNCH_ACTIVITY         = 100;
            //其中msg.obj=ActivityClientRecord
            if(message.what==100){
                Object obj = message.obj;
                try {
                    //从ActivityClientRecord中将代理信息替换回来
                    Field intentField  = obj.getClass().getDeclaredField("intent");
                    if(!intentField.isAccessible()){
                        intentField.setAccessible(true);
                    }
                    Intent proxyintent = (Intent) intentField.get(obj);
                    Intent orginintent = proxyintent.getParcelableExtra(ORGIN_INTENT);
                    if(orginintent!=null){
                        proxyintent.setComponent(orginintent.getComponent());
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            //让消息分发继续前进
            handler.handleMessage(message);
            return true;
        }
    }



}
