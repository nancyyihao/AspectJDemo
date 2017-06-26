
package com.netease.demo.aspectj;


import android.util.Log;

import com.netease.demo.BaseApplication;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 对com.netease.demo包名下面的都全部进行代码插桩
 *
 */
@Aspect
public class TraceAspect {

    private static final String POINTCUT_METHOD =
            "execution(* com.netease.demo..*.*(..))";
            //+ "&& !execution(* android.app.Application+.*(..))";
    /**
     * 截获原方法，并替换
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around(POINTCUT_METHOD)
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {

        long start  = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        if (Thread.currentThread().getId() == BaseApplication.sUiThreadId) {
            Log.e("Aspectj", joinPoint.getSignature().getName() + " 总用时：" + (end-start) + "ms");
        }

        return result;
    }
//
//    @Pointcut(value = "execution(* com.netease.demo.component..*.*(..))")
//    private void otherPointCut() {}
//
//    @Around(value = "otherPointCut()")
//    public Object weaveOther(ProceedingJoinPoint joinPoint) throws Throwable {
//        return null;
//    }

}
