package com.datlichkhambenh.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.*;
import org.springframework.amqp.rabbit.connection.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitMqConfig {
    @Bean
    Queue emailQueue(@Value("${app.messaging.email-queue}") String queueName,
            @Value("${app.messaging.email-exchange}") String exchangeName) {
        return QueueBuilder.durable(queueName).deadLetterExchange(exchangeName + ".dlx")
                .deadLetterRoutingKey("dead").build();
    }
    @Bean
    DirectExchange emailExchange(@Value("${app.messaging.email-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }
    @Bean
    Binding emailBinding(@Qualifier("emailQueue") Queue queue, @Qualifier("emailExchange") DirectExchange exchange,
            @Value("${app.messaging.email-routing-key}") String key) {
        return BindingBuilder.bind(queue).to(exchange).with(key);
    }
    @Bean
    DirectExchange emailDeadExchange(@Value("${app.messaging.email-exchange}") String name) {
        return new DirectExchange(name + ".dlx", true, false);
    }
    @Bean
    Queue emailDeadQueue(@Value("${app.messaging.email-queue}") String name) { return new Queue(name + ".dlq", true); }
    @Bean
    Binding emailDeadBinding(@Qualifier("emailDeadQueue") Queue queue, @Qualifier("emailDeadExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("dead");
    }
    @Bean
    Jackson2JsonMessageConverter rabbitMessageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        if (connectionFactory instanceof CachingConnectionFactory factory) {
            factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            factory.setPublisherReturns(true);
        }
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        // Fixed safe failure log: Spring's default recoverer logs the complete message body (PHI/tokens).
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxRetries(2)
                .backOffOptions(500, 2.0, 2000)
                .recoverer((message, cause) -> { throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
                        "Notification delivery exhausted retry limit"); }).build());
        // Suppress payload/exception logging from the container's default error handler.
        factory.setErrorHandler(error -> { throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
                "Notification rejected; inspect delivery metrics and dead-letter queue"); });
        return factory;
    }
}
