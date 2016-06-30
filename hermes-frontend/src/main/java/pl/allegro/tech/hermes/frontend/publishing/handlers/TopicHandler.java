package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.TopicWithMetrics;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import java.util.Optional;
import java.util.function.Consumer;

import static pl.allegro.tech.hermes.api.ErrorCode.GROUP_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

class TopicHandler implements HttpHandler {

    private final HttpHandler next;
    private final TopicsCache topicsCache;
    private final MessageErrorProcessor messageErrorProcessor;

    TopicHandler(HttpHandler next, TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor) {
        this.next = next;
        this.topicsCache = topicsCache;
        this.messageErrorProcessor = messageErrorProcessor;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            // switch to worker thread
            exchange.dispatch(this);
            return;
        }

        String messageId = MessageIdGenerator.generate();

        onTopicPresent(exchange, messageId, topicWithMetrics -> {
            exchange.addExchangeCompleteListener(new ExchangeMetrics(topicWithMetrics));
            exchange.putAttachment(AttachmentContent.KEY, new AttachmentContent(topicWithMetrics, new MessageState(), messageId));
            try {
                next.handleRequest(exchange);
            } catch (Exception e) {
                messageErrorProcessor.sendAndLog(exchange, topicWithMetrics.getTopic(), messageId, e);
            }
        });
    }

    private void onTopicPresent(HttpServerExchange exchange, String messageId, Consumer<TopicWithMetrics> consumer) {
        TopicName topicName = fromQualifiedName(exchange.getQueryParameters().get("qualifiedTopicName").getFirst());
        try {
            Optional<TopicWithMetrics> topic = topicsCache.getTopic(topicName);
            if (topic.isPresent()) {
                consumer.accept(topic.get());
                return;
            }
            nonExistentTopic(exchange, topicName.qualifiedName(), messageId);
        } catch (IllegalArgumentException exception) {
            missingTopicGroup(exchange, topicName.qualifiedName(), messageId);
        }
    }

    public void missingTopicGroup(HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
        messageErrorProcessor.sendQuietly(
                exchange,
                error("Missing valid topic group in path. Found " + qualifiedTopicName, GROUP_NOT_EXISTS),
                messageId);
    }

    public void nonExistentTopic(HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
        messageErrorProcessor.sendQuietly(
                exchange,
                error("Topic not found: " + qualifiedTopicName, TOPIC_NOT_EXISTS),
                messageId);
    }
}