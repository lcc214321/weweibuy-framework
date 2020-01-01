package com.weweibuy.framework.rocketmq.support;

import com.weweibuy.framework.rocketmq.annotation.RocketProvider;
import com.weweibuy.framework.rocketmq.annotation.RocketProviderHandler;
import com.weweibuy.framework.rocketmq.core.RocketMethodMetadata;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 目标方法元数据解析器
 *
 * @author durenhao
 * @date 2019/12/30 20:47
 **/
public class TargetMethodMetaDataParser {

    private final RocketMethodMetadataFactory methodMetadataFactory;

    private final MessageBodyParameterProcessor methodParameterProcessor;

    private final List<AnnotatedParameterProcessor> annotatedParameterProcessor;

    public TargetMethodMetaDataParser(RocketMethodMetadataFactory methodMetadataFactory, MessageBodyParameterProcessor methodParameterProcessor,
                                      List<AnnotatedParameterProcessor> annotatedParameterProcessor) {
        this.methodMetadataFactory = methodMetadataFactory;
        this.methodParameterProcessor = methodParameterProcessor;
        this.annotatedParameterProcessor = annotatedParameterProcessor;
    }


    public Map<Method, RocketMethodMetadata> parser(Class<?> target) {

        return Arrays.stream(target.getMethods())
                .filter(m -> !shouldFilterMethod(m))
                .filter(m -> Objects.nonNull(m.getAnnotation(RocketProviderHandler.class)))
                .map(m -> {
                    RocketMethodMetadata rocketMethodMetadata = methodMetadataFactory.newInstance(target, m);
                    parseAnnotationOnMethod(rocketMethodMetadata, m);
                    return parseAnnotationOnParameter(rocketMethodMetadata, m);
                })
                .peek(m -> Assert.isTrue(m.getBodyIndex() != null, m.getMethod().getName() + ", 无法匹配消息体!"))
                .collect(Collectors.toMap(RocketMethodMetadata::getMethod, i -> i));

    }

    protected RocketMethodMetadata parseAnnotationOnClass(RocketMethodMetadata methodMetadata, Class<?> target) {
        Class<?>[] interfaces = target.getInterfaces();
        RocketProvider annotation = interfaces[0].getAnnotation(RocketProvider.class);
        String topic = annotation.topic();
        methodMetadata.setTopic(topic);
        return methodMetadata;
    }

    protected RocketMethodMetadata parseAnnotationOnMethod(RocketMethodMetadata metadata, Method method) {
        RocketProviderHandler providerHandler = method.getAnnotation(RocketProviderHandler.class);
        metadata.setMethod(method);
        metadata.setTag(providerHandler.tag());
        metadata.setKeyExpression(providerHandler.key());
        metadata.setOneWay(providerHandler.oneWay());
        metadata.setOrderly(providerHandler.orderly());
        metadata.setTimeout(providerHandler.timeout());
        metadata.setBatch(providerHandler.batch());
        return metadata;
    }


    private RocketMethodMetadata parseAnnotationOnParameter(RocketMethodMetadata metadata, Method method, Annotation[] annotations, int index) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (annotations.length == 0) {
            methodParameterProcessor.buildMetadata(metadata, parameterTypes[index], index);
        } else {
            boolean annotationsMatched = false;
            for (int i = 0; i < annotations.length; i++) {
                for (int j = 0; j < annotatedParameterProcessor.size(); j++) {
                    if (annotatedParameterProcessor.get(j).match(annotations[i])) {
                        Assert.isTrue(!annotationsMatched, method.getName() + " 第" + index + "参数,匹配多个处理器!");
                        annotatedParameterProcessor.get(j).buildMetadata(metadata, parameterTypes[index], index);
                        annotationsMatched = true;
                    }
                }
            }
            Assert.isTrue(annotationsMatched, method.getName() + " 第" + index + "参数,无法匹配处理!");
        }
        return metadata;
    }


    protected RocketMethodMetadata parseAnnotationOnParameter(RocketMethodMetadata metadata, Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        Assert.isTrue(annotations.length != 0, "方法: " + metadata.getMethod().getDeclaringClass().getSimpleName() + "."
                + metadata.getMethod().getName() + " 方法不能为空参");
        for (int i = 0; i < annotations.length; i++) {
            parseAnnotationOnParameter(metadata, method, annotations[i], i);
        }
        return metadata;
    }


    private boolean shouldFilterMethod(Method method) {
        return method.getDeclaringClass() == Object.class ||
                (method.getModifiers() & Modifier.STATIC) != 0 ||
                MethodUtils.isDefault(method);
    }


}