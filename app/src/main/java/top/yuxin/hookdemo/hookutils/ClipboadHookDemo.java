package top.yuxin.hookdemo.hookutils;

import android.content.ClipData;
import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by yuxin-hu on 17-11-29.
 */

public class ClipboadHookDemo {
    private static final  String TAG=ClipboadHookDemo.class.getSimpleName();

    public static void hookService(){

        ServicesHook servicesHook= new ServicesHook(Context.CLIPBOARD_SERVICE,"android.content.IClipboard",true);
        servicesHook.hookService(new InvocationHandler() {
            @Override
            public Object invoke(Object proxyObj, Method method, Object[] objects) throws Throwable {
                String methodName = method.getName();
                int argsLength = objects.length;
                //每次从本应用复制的文本，后面都加上出处
                if ("setPrimaryClip".equals(methodName)) {
                    if (argsLength >= 2 && objects[0] instanceof ClipData) {
                        ClipData data = (ClipData) objects[0];
                        String text = data.getItemAt(0).getText().toString();
                        text += "this is shared from ClipboadHookDemo";
                        objects[0] = ClipData.newPlainText(data.getDescription().getLabel(), text);
                    }
                }
                return method.invoke(proxyObj, objects);
            }
        });

    }




}
