package com.licenta.model;

import org.apache.mahout.cf.taste.recommender.IDRescorer;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Entity
public class Movie {

    @Id
    @Column(name = "movie_id", insertable=true, updatable=true, unique=true, nullable=false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer movieId;
    private String title;
    private Integer year;

    private boolean exclude = false;

    @OneToOne
    @JoinColumn(name = "link_id")
    private Link link;

    @OneToMany(mappedBy="movie",targetEntity=Tag.class, fetch= FetchType.LAZY)
    private List<Tag> tags = new ArrayList<>();

    public Movie(Integer movieId, String title, Integer year) {
        this.title = title;
        this.year = year;
        this.movieId = movieId;
    }

    public Movie(){}

    @OneToMany(
            mappedBy = "movie",
            cascade = CascadeType.MERGE
    )
    private List<Rating> ratings = new ArrayList<>();

    public void addRating(User user, double ratingScore, long timestamp){
        if(timestamp == 0){
            timestamp = new Date().getTime();
        }
        Rating rating = new Rating(this, user, ratingScore, timestamp);

        ratings.add(rating);
        user.getRatings().add(rating);
    }

    public void removeRating(User user){

        for(Iterator<Rating> iterator = ratings.iterator(); iterator.hasNext();){

            Rating rating = iterator.next();

            if(rating.getMovie().equals(this) && rating.getUser().equals(user)){

                iterator.remove();
                rating.getUser().getRatings().remove(rating);

                rating.setMovie(null);
                rating.setUser(null);
            }

        }

    }

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres = new ArrayList<>();

    public void addGenre(Genre genre) {
        genres.add(genre);
        genre.getMovies().add(this);
    }

    public void removeGenre(Genre genre) {
        genres.remove(genre);
        genre.getMovies().remove(this);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.setMovie(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.setMovie(null);
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }
}
