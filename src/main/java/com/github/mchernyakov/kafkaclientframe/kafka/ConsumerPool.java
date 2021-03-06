package com.github.mchernyakov.kafkaclientframe.kafka;

import com.github.mchernyakov.kafkaclientframe.util.ThreadUtil;
import com.github.mchernyakov.kafkaclientframe.process.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ConsumerPool<K, V> implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(ConsumerPool.class.getName());

    private final ExecutorService executorService;
    private final List<Consumer<K, V>> consumers;
    private final List<? extends Processor<K, V>> processors;
    private final List<String> topics;
    private final Properties consumerProperties;
    private final int numConsumers;

    public ConsumerPool(int numConsumers, List<? extends Processor<K, V>> processors, List<String> topics, Properties consumerProperties) {
        this.numConsumers = numConsumers;
        this.processors = processors;
        this.topics = topics;
        this.consumerProperties = consumerProperties;
        this.consumers = new ArrayList<>();

        ThreadFactory threadFactory = ThreadUtil.threadFactory("consumer", false);
        this.executorService = Executors.newFixedThreadPool(numConsumers, threadFactory);
    }

    public void start() {
        logger.info("Start consumers...");
        for (int i = 0; i < numConsumers; i++) {
            Consumer<K, V> consumer = new Consumer<>(
                    i,
                    consumerProperties,
                    topics,
                    processors
            );

            consumers.add(consumer);
            executorService.submit(consumer);
        }
    }

    @Override
    public void close() throws Exception {
        consumers.forEach(Consumer::shutdown);
        ThreadUtil.shutdownExecutorService(executorService);
    }
}
