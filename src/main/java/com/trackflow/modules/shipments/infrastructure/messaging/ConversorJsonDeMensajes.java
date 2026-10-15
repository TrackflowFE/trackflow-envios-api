package com.trackflow.modules.shipments.infrastructure.messaging;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los mensajes de RabbitMQ viajan como JSON. En el monolito este conversor se definía en
 * la configuración de logística; la API de envíos necesita el suyo.
 */
@Configuration
class ConversorJsonDeMensajes {

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
