package com.improving.demo;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.time.Duration;

import com.improving.demo.avro.Movie;
import com.improving.demo.avro.RatedMovie;
import com.improving.demo.avro.Rating;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class StreamProcessor {

    public Topology buildTopology(Properties allProps) {
        final StreamsBuilder builder = new StreamsBuilder();
        final String movieTopic = allProps.getProperty("movie.topic.name");
        final String rekeyedMovieTopic = allProps.getProperty("rekeyed.movie.topic.name");
        final String ratingTopic = allProps.getProperty("rating.topic.name");
        final String ratedMoviesTopic = allProps.getProperty("rated.movies.topic.name");
        final String recentMoviesTopic = allProps.getProperty("recent.movies.topic.name");
        final MovieRatingJoiner joiner = new MovieRatingJoiner();

        KStream<String, Movie> movieStream = builder.<String, Movie>stream(movieTopic)
                .map((key, movie) -> new KeyValue<>(String.valueOf(movie.getId()), movie));

        movieStream.to(rekeyedMovieTopic);

        KTable<String, Movie> movies = builder.table(rekeyedMovieTopic);

        KStream<String, Rating> ratings = builder.<String, Rating>stream(ratingTopic)
                .map((key, rating) -> new KeyValue<>(String.valueOf(rating.getId()), rating));

        KStream<String, RatedMovie> ratedMovies = ratings.join(movies, joiner);
        ratedMovies.to(ratedMoviesTopic, Produced.with(Serdes.String(), ratedMovieAvroSerde(allProps)));

        KStream<String, RatedMovie> recentMovies = builder.<String, RatedMovie>stream(ratedMoviesTopic)
            .filter((k,v) -> v.getReleaseYear() >= 2010);
        recentMovies.to(recentMoviesTopic, Produced.with(Serdes.String(), ratedMovieAvroSerde(allProps)));

        return builder.build();
    }

    private SpecificAvroSerde<RatedMovie> ratedMovieAvroSerde(Properties allProps) {
        SpecificAvroSerde<RatedMovie> movieAvroSerde = new SpecificAvroSerde<>();
        movieAvroSerde.configure((Map)allProps, false);
        return movieAvroSerde;
    }

    public void createTopics(Properties allProps) {
        AdminClient client = AdminClient.create(allProps);
        List<NewTopic> topics = new ArrayList<>();
        topics.add(new NewTopic(
                allProps.getProperty("movie.topic.name"),
                Integer.parseInt(allProps.getProperty("movie.topic.partitions")),
                Short.parseShort(allProps.getProperty("movie.topic.replication.factor"))));

        topics.add(new NewTopic(
                allProps.getProperty("rekeyed.movie.topic.name"),
                Integer.parseInt(allProps.getProperty("rekeyed.movie.topic.partitions")),
                Short.parseShort(allProps.getProperty("rekeyed.movie.topic.replication.factor"))));

        topics.add(new NewTopic(
                allProps.getProperty("rating.topic.name"),
                Integer.parseInt(allProps.getProperty("rating.topic.partitions")),
                Short.parseShort(allProps.getProperty("rating.topic.replication.factor"))));

        topics.add(new NewTopic(
                allProps.getProperty("rated.movies.topic.name"),
                Integer.parseInt(allProps.getProperty("rated.movies.topic.partitions")),
                Short.parseShort(allProps.getProperty("rated.movies.topic.replication.factor"))));

        topics.add(new NewTopic(
                allProps.getProperty("recent.movies.topic.name"),
                Integer.parseInt(allProps.getProperty("recent.movies.topic.partitions")),
                Short.parseShort(allProps.getProperty("recent.movies.topic.replication.factor"))));
    
        client.createTopics(topics);
        client.close();
    }

    public Properties loadEnvProperties(String fileName) throws IOException {
        Properties allProps = new Properties();
        FileInputStream input = new FileInputStream(fileName);
        allProps.load(input);
        input.close();

        return allProps;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("This program takes one argument: the path to an environment configuration file.");
        }

        StreamProcessor ts = new StreamProcessor();
        Properties allProps = ts.loadEnvProperties(args[0]);
        allProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        allProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        Topology topology = ts.buildTopology(allProps);

        ts.createTopics(allProps);

        final KafkaStreams streams = new KafkaStreams(topology, allProps);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}