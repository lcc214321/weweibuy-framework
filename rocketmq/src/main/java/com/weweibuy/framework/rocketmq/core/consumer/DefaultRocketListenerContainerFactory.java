package com.weweibuy.framework.rocketmq.core.consumer;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RocketListenerContainerFactory 默认实现
 *
 * @author durenhao
 * @date 2020/1/11 22:05
 **/
public class DefaultRocketListenerContainerFactory implements RocketListenerContainerFactory {


    @Override
    public RocketListenerContainer createListenerContainer(List<MethodRocketListenerEndpoint> endpointList) {

        boolean orderly = endpointList.get(0).getOrderly();
        RocketListenerContainer container = null;
        if (orderly) {
            container = createOrderlyContainer(endpointList);
        }else {
            container = createConcurrentlyContainer(endpointList);
        }
        List<RocketMessageListener> listenerList = createListener(endpointList, container);
        container.setListeners(listenerList);
        return container;
    }


    private List<RocketMessageListener> createListener(List<MethodRocketListenerEndpoint> endpointList, RocketListenerContainer container) {
        return endpointList.stream()
                .map(endpoint -> endpoint.createRocketMessageListener(container))
                .collect(Collectors.toList());

    }

    private RocketListenerContainer createOrderlyContainer(List<MethodRocketListenerEndpoint> endpointList) {
        if (endpointList.size() == 1) {
            MethodRocketListenerEndpoint endpoint = endpointList.get(0);
            return new OrderlyRocketListenerContainer(createMqConsumer(endpoint, endpoint.getTags()), endpoint.getConsumeMessageBatchMaxSize(), endpoint.getBatchHandlerModel());
        }
        String tags = mergeTags(endpointList);
        return new OrderlyRocketListenerContainer(createMqConsumer(endpointList.get(0), tags), endpointList.get(0).getConsumeMessageBatchMaxSize(), endpointList.get(0).getBatchHandlerModel());
    }


    private RocketListenerContainer createConcurrentlyContainer(List<MethodRocketListenerEndpoint> endpointList) {
        if (endpointList.size() == 1) {
            MethodRocketListenerEndpoint endpoint = endpointList.get(0);
            return new ConcurrentlyRocketListenerContainer(createMqConsumer(endpoint, endpoint.getTags()), endpoint.getConsumeMessageBatchMaxSize(), endpoint.getBatchHandlerModel());
        }
        String tags = mergeTags(endpointList);
        return new ConcurrentlyRocketListenerContainer(createMqConsumer(endpointList.get(0), tags), endpointList.get(0).getConsumeMessageBatchMaxSize(), endpointList.get(0).getBatchHandlerModel());
    }

    private DefaultMQPushConsumer createMqConsumer(MethodRocketListenerEndpoint endpoint, String tags) {
        RPCHook rpcHook = null;
        DefaultMQPushConsumer pushConsumer = new DefaultMQPushConsumer(endpoint.getGroup(), rpcHook, new AllocateMessageQueueAveragely(),
                Optional.ofNullable(endpoint.getMsgTrace()).orElse(false), endpoint.getTraceTopic());
        pushConsumer.setNamesrvAddr(endpoint.getNameServer());
        pushConsumer.setAccessChannel(endpoint.getAccessChannel());
        pushConsumer.setConsumeThreadMin(endpoint.getThreadMin());
        pushConsumer.setConsumeThreadMin(endpoint.getThreadMax());
        pushConsumer.setConsumeTimeout(endpoint.getTimeout());
        pushConsumer.setMessageModel(endpoint.getMessageModel());
        pushConsumer.setConsumeMessageBatchMaxSize(endpoint.getConsumeMessageBatchMaxSize());

        try {
            pushConsumer.subscribe(endpoint.getTopic(), tags);
        } catch (MQClientException e) {
            throw new IllegalArgumentException(e);
        }
        return pushConsumer;
    }


    private String mergeTags(List<MethodRocketListenerEndpoint> endpointList) {
        return endpointList.stream()
                .map(MethodRocketListenerEndpoint::getTags)
                .reduce((a, b) -> a + "||" + b + "||")
                .map(s -> s.substring(0, s.length() - 2))
                .orElseThrow(() -> new IllegalArgumentException("Tag 错误"));
    }


}
