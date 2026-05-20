package com.campussync.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.jobs.mode", havingValue = "rabbit")
public class QueueTopologyConfig {

    public static final String JOBS_EXCHANGE = "campussync.jobs";
    public static final String EMAIL_QUEUE = "campussync.jobs.email";
    public static final String NOTIFICATION_QUEUE = "campussync.jobs.notification";
    public static final String CERTIFICATE_QUEUE = "campussync.jobs.certificate";

    @Bean
    DirectExchange jobsExchange() {
        return new DirectExchange(JOBS_EXCHANGE, true, false);
    }

    @Bean
    Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    Queue certificateQueue() {
        return new Queue(CERTIFICATE_QUEUE, true);
    }

    @Bean
    Binding emailBinding(Queue emailQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(emailQueue).to(jobsExchange).with("email");
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(notificationQueue).to(jobsExchange).with("notification");
    }

    @Bean
    Binding certificateBinding(Queue certificateQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(certificateQueue).to(jobsExchange).with("certificate");
    }
}
