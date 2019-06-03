package com.licenta.ui.views;

import javafx.scene.image.Image;

public class RecommendedMovieView {

    private String title;
    private Image image;
    private Long ratingValue;
    private int movieId;

    public RecommendedMovieView(String title, Image image, Long ratingValue, int movieId) {
        this.title = title;
        this.image = image;
        this.ratingValue = ratingValue;
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Long getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(Long ratingValue) {
        this.ratingValue = ratingValue;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }
}
