package com.pocketdigi.PLib.annotation;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import com.pocketdigi.PLib.core.*;
import com.pocketdigi.PLib.exception.FindViewError;
import com.pocketdigi.PLib.exception.InflatError;

import java.lang.reflect.Field;

/**
 *
 * Created by fhp on 14-9-5.
 */
public class ViewInjector {
    public static InjectAdapter inject(PActivity activity)
    {
        InjectAdapter adapter=new ActivityInjectAdapter();
        inject(activity,adapter);
        return adapter;
    }
    public static InjectAdapter inject(PFragmentActivity activity)
    {
        InjectAdapter adapter=new ActivityInjectAdapter();
        inject(activity,adapter);
        return adapter;
    }
    public static InjectAdapter inject(PFragment fragment)
    {
        InjectAdapter adapter=new PFragmentInjectAdapter();
        inject(fragment,adapter);
        return adapter;
    }
    public static InjectAdapter inject(PDialogFragment fragment)
    {
        InjectAdapter adapter=new PFragmentInjectAdapter();
        inject(fragment,adapter);
        return adapter;
    }

    /**
     * 在Fragment中使用时，在onViewCreated里调用
     * @param obj
     * @param adapter
     */
    public static void findViewById(Object obj,InjectAdapter adapter)
    {
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field:fields)
        {
            field.setAccessible(true);
            if(View.class.isAssignableFrom(field.getType()))
            {
                if(field.isAnnotationPresent(ViewById.class))
                {
                    ViewById viewById=field.getAnnotation(ViewById.class);
                    int resId=viewById.value();
                    if(resId==0)
                    {
                        throw new FindViewError(field);
                    }
                    try {
                        field.set(obj,adapter.findViewValue(obj, resId));
                    } catch (IllegalAccessException e) {
                        PLog.e("ViewInjector","FindView Error for "+field);
                        e.printStackTrace();
                    }
                }

            }else if(Fragment.class.isAssignableFrom(field.getType())||android.app.Fragment.class.isAssignableFrom(field.getType())){
                //两种Fragment
                if(field.isAnnotationPresent(FragmentById.class)){
                    FragmentById fragmentById=field.getAnnotation(FragmentById.class);
                    int resId=fragmentById.value();
                    if(resId==0)
                    {
                        throw new FindViewError(field);
                    }
                    try {
                        field.set(obj,adapter.findFragmentValue(obj,resId));
                    } catch (IllegalAccessException e) {
                        PLog.e("ViewInjector","Find Fragment Error for "+field);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     * private 避免非指定类型参数传入
     * @param obj
     */
    private static void inject(Object obj,InjectAdapter adapter)
    {
        Class aClass = obj.getClass();
        //setContentView
        if(aClass.isAnnotationPresent(Inflat.class))
        {
           Inflat inflat= (Inflat) aClass.getAnnotation(Inflat.class);
           int layoutId=inflat.value();
           if(layoutId==0)
                throw new InflatError("You must set a Inflat annotation for this activity or fragment");
            adapter.inflatLayout(obj,layoutId);
        }
        if(Activity.class.isAssignableFrom(aClass))
        {
            findViewById(obj,adapter);
        }
    }



}