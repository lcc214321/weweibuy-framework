package com.weweibuy.framework.compensate.interceptor;


import com.weweibuy.framework.common.core.utils.Jdk9Option;
import com.weweibuy.framework.compensate.annotation.Compensate;
import com.weweibuy.framework.compensate.annotation.Propagation;
import com.weweibuy.framework.compensate.core.CompensateAlarmService;
import com.weweibuy.framework.compensate.core.CompensateStore;
import com.weweibuy.framework.compensate.model.CompensateInfo;
import com.weweibuy.framework.compensate.support.CompensateAnnotationMetaDataParser;
import com.weweibuy.framework.compensate.support.CompensateContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.ExecutorService;

/**
 * 补偿切面
 *
 * @author durenhao
 * @date 2020/2/13 21:06
 **/
public class CompensateInterceptor implements MethodInterceptor {

    private final CompensateStore compensateStore;

    private final CompensateAnnotationMetaDataParser metaDataParser;

    private ExecutorService executorService;

    private CompensateAlarmService compensateAlarmService;

    public CompensateInterceptor(CompensateStore compensateStore,
                                 CompensateAnnotationMetaDataParser metaDataParser, CompensateAlarmService compensateAlarmService) {
        this.compensateStore = compensateStore;
        this.metaDataParser = metaDataParser;
        this.compensateAlarmService = compensateAlarmService;
    }

    public CompensateInterceptor(CompensateStore compensateStore, CompensateAnnotationMetaDataParser metaDataParser,
                                 ExecutorService executorService, CompensateAlarmService compensateAlarmService) {
        this.compensateStore = compensateStore;
        this.metaDataParser = metaDataParser;
        this.executorService = executorService;
        this.compensateAlarmService = compensateAlarmService;
    }


    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        boolean bind = false;
        Boolean compensate = CompensateContext.getCompensate();
        if (compensate == null) {
            CompensateContext.setCompensate();
            bind = true;
        }
        try {
            return methodInvocation.proceed();
        } catch (Exception e) {
            Compensate annotation = methodInvocation.getMethod().getAnnotation(Compensate.class);
            Boolean bindCompensate = CompensateContext.getCompensate();
            if (bind || bindCompensate == null || annotation.propagation().equals(Propagation.REQUIRES_NEW)) {
                Jdk9Option.ofNullable(executorService)
                        .ifPresentOrElse(es -> es.execute(() -> parseAndSaveCompensateInfo(methodInvocation, e)),
                                () -> parseAndSaveCompensateInfo(methodInvocation, e));
            }
            throw e;
        } finally {
            if (bind) {
                CompensateContext.removeCompensate();
            }
        }
    }

    private void parseAndSaveCompensateInfo(MethodInvocation methodInvocation, Exception e) {
        Compensate annotation = methodInvocation.getMethod().getAnnotation(Compensate.class);
        if (metaDataParser.shouldCompensate(annotation, e)) {
            CompensateInfo compensateInfo = metaDataParser.toCompensateInfo(annotation, methodInvocation.getThis(),
                    methodInvocation.getMethod(), methodInvocation.getArguments());
            try {
                compensateStore.saveCompensateInfo(compensateInfo);
            } catch (Exception e1) {
                compensateAlarmService.sendSaveCompensateAlarm(compensateInfo, e1);
            }
        }
    }
}
