package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FraudDetectorService {

    private final KafkaDispatcher<Order> orderDispatcher = new KafkaDispatcher<Order>();

    public static void main(String[] args) {
        var fraudService = new FraudDetectorService();

        try (var service = new KafkaService<>(
                FraudDetectorService.class.getSimpleName(),
                "ECOMMERCE_NEW_ORDER",
                fraudService::parse,
                Order.class,
                Map.of())) {

            service.run();
        }
    }

    private boolean isFraud(Order order) {
        return order.getAmount().compareTo(BigDecimal.valueOf(4500)) >= 0;
    }

    private void parse(ConsumerRecord<String, Order> record) throws ExecutionException, InterruptedException {
        System.out.println("-----------------------------------------");
        System.out.println("Processing new order, checking for fraud");
        System.out.println(record.key());
        System.out.println(record.value());
        System.out.println(record.partition());
        System.out.println(record.offset());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        var order = record.value();

        if (isFraud(order)) {
            // pretending that the fraud happens when the amount is >= 4500
            System.out.println("Order is a fraud!!!" + order);
            orderDispatcher.send("ECOMMERCE_ORDER_REJECTED", order.getUserId(), order);
        } else {
            orderDispatcher.send("ECOMMERCE_ORDER_APPROVED", order.getUserId(), order);
            System.out.println("Approved: " + order);
        }

        System.out.println("Order processed");
    }
}
