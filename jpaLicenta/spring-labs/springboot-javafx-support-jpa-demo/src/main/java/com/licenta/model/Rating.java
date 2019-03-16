package com.licenta.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class Rating {

    @EmbeddedId
    private RatingId ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    private Double rating;
    private Long timestamp;

    public Rating(Movie movie, User user, Double rating, Long timestamp) {
        this.user = user;
        this.movie = movie;
        this.ratingId = new RatingId(movie.getMovieId(), user.getUserId());
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public Rating(){}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Rating that = (Rating) o;
        return Objects.equals(movie, that.movie) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, movie);
    }

    public RatingId getRatingId() {
        return ratingId;
    }

    public void setRatingId(RatingId ratingId) {
        this.ratingId = ratingId;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }


}
