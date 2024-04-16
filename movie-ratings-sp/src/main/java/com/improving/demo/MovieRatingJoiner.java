package com.improving.demo;

import org.apache.kafka.streams.kstream.ValueJoiner;
import com.improving.demo.avro.Rating;
import com.improving.demo.avro.Movie;
import com.improving.demo.avro.RatedMovie;

public class MovieRatingJoiner implements ValueJoiner<Rating, Movie, RatedMovie>{
    
  public RatedMovie apply(Rating rating, Movie movie) {
    System.out.println("Rating Movie: " + movie.getTitle());

    return RatedMovie.newBuilder()
        .setId(movie.getId())
        .setTitle(movie.getTitle())
        .setReleaseYear(movie.getReleaseYear())
        .setRating(rating.getRating())
        .build();
  }

}
