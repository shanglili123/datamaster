
package com.datamaster.module.collector.config;

import com.datamaster.module.collector.listener.ProcessListener;
import com.datamaster.module.collector.listener.TaskListener;
import com.datamaster.module.collector.listener.TaskLogListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ProcessListener processListener,
            TaskListener taskListener,
            TaskLogListener taskLogListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(new MessageListenerAdapter(processListener),
                new ChannelTopic("ds:channel:processInstance"));

        container.addMessageListener(new MessageListenerAdapter(taskListener),
                new ChannelTopic("ds:channel:taskInstance:insert"));
        container.addMessageListener(new MessageListenerAdapter(taskListener),
                new ChannelTopic("ds:channel:taskInstance:update"));

        container.addMessageListener(new MessageListenerAdapter(taskLogListener),
                new ChannelTopic("ds:channel:taskInstance:log"));

        return container;
    }
}
