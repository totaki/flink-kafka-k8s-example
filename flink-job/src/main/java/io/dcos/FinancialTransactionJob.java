package io.dcos;
// --kafka_host my-rnd-1-kafka-bootstrap:9092
import java.time.Duration;
import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;

import java.util.Properties;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class FinancialTransactionJob {

    public static void main(String[] args) throws Exception {

        // Set parameters
        ParameterTool parameters = ParameterTool.fromArgs(args);
        String inputTopic = parameters.get("inputTopic", "transactions");
        String outputTopic = parameters.get("outputTopic", "fraud");
        String kafkaHost = parameters.get("kafka_host", "broker.kafka.l4lb.thisdcos.directory:9092");

        // create execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaHost)
                .setTopics(inputTopic)
                .setGroupId("flink_consumer_1")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<Transaction> transactionsStream = env
                .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
                // Map from String from Kafka Stream to Transaction.
                .map(value -> new Transaction(value));

        transactionsStream.print();

        // Extract timestamp information to support 'event-time' processing
        DataStream<Transaction> timestampedStream = transactionsStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Transaction>forBoundedOutOfOrderness(Duration.ZERO)
                                .withTimestampAssigner((element, recordTimestamp) -> element.getTimestamp())
                );

        DataStream<TransactionAggregate> rateCount = timestampedStream
                .keyBy(transaction -> transaction.getOrigin() + "_" + transaction.getTarget())
                // Sum over ten minutes with a two-minute slide
                .window(SlidingEventTimeWindows.of(Time.minutes(10), Time.minutes(2)))
                .aggregate(new TransactionAggregator());

        DataStream<String> kafkaOutput = rateCount
                .filter(transactionAggregate -> transactionAggregate.getAmount() > 10000)
                .map(TransactionAggregate::toString);

        rateCount.print();

        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers(kafkaHost)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(outputTopic)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();

        kafkaOutput.sinkTo(kafkaSink);

        env.execute("FinancialTransactionJob");
    }

    // Aggregator for TransactionAggregate
    private static class TransactionAggregator implements AggregateFunction<Transaction, TransactionAggregate, TransactionAggregate> {

        @Override
        public TransactionAggregate createAccumulator() {
            return new TransactionAggregate();
        }

        @Override
        public TransactionAggregate add(Transaction transaction, TransactionAggregate accumulator) {
            accumulator.getTransactionVector().add(transaction);
            accumulator.setAmount(accumulator.getAmount() + transaction.getAmount());
            accumulator.setStartTimestamp(min(accumulator.getStartTimestamp(), transaction.getTimestamp()));
            accumulator.setEndTimestamp(max(accumulator.getEndTimestamp(), transaction.getTimestamp()));
            return accumulator;
        }

        @Override
        public TransactionAggregate getResult(TransactionAggregate accumulator) {
            return accumulator;
        }

        @Override
        public TransactionAggregate merge(TransactionAggregate a, TransactionAggregate b) {
            a.getTransactionVector().addAll(b.getTransactionVector());
            a.setAmount(a.getAmount() + b.getAmount());
            a.setStartTimestamp(min(a.getStartTimestamp(), b.getStartTimestamp()));
            a.setEndTimestamp(max(a.getEndTimestamp(), b.getEndTimestamp()));
            return a;
        }
    }
}
