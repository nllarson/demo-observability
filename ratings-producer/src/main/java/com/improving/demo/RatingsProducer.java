package com.improving.demo;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.Callback;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.improving.demo.avro.Rating;


public class RatingsProducer {

    private final Producer<String, GenericRecord> producer;
    private String topic;

    public RatingsProducer(final Producer<String, GenericRecord> producer, final String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    private static double generateRating(double lower , double upper ,int decimal_places)
    {
        double result = (double) (Math.random() * (upper - lower)) + lower;
        return Double.parseDouble( String.format("%."+ decimal_places +"f",result));
    }

    private static int getRandomMovie(int[] array) {
        int rnd = (int)(Math.random()*array.length);
        return array[rnd];
    }

    public static Properties loadEnvProperties(String fileName) throws IOException {
        Properties allProps = new Properties();
        FileInputStream input = new FileInputStream(fileName);
        allProps.load(input);
        input.close();

        return allProps;
    }

    public Future<RecordMetadata> produceRating(Rating rating) {
        ProducerRecord <String, GenericRecord> producerRecord = new ProducerRecord<>(this.topic, null, rating);
        return this.producer.send(producerRecord);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("This program takes one argument: the path to an environment configuration file.");
        }

        Properties allProps = RatingsProducer.loadEnvProperties(args[0]);
        allProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        allProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        
        final KafkaProducer<String, GenericRecord> producer = new KafkaProducer(allProps);
        final RatingsProducer app = new RatingsProducer(producer, allProps.getProperty("rating.topic.name"));

        int[] movies = {294,354,782,128,780};


        Runtime.getRuntime().addShutdownHook(new Thread(producer::close, "Shutdown-thread"));

        while(true) {
            // produce random records every 30 seconds
            Thread.sleep(30000);
            
            // find random movie
            int movie = getRandomMovie(movies);
            // makeup random rating
            double score = generateRating(6, 10, 1);
            // produce rating
            app.produceRating(Rating.newBuilder().setId(movie).setRating(score).build());

            System.out.println("Rated Movie: " + movie + " - " + score + ".");
        }




    }
}
