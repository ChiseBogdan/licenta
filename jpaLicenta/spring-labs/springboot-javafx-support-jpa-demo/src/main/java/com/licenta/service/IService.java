package com.licenta.service;

import com.licenta.ui.views.RecommendedMovieView;
import javafx.scene.image.Image;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.util.List;

public interface IService {

    List<RecommendedMovieView> getMahoutRecommendations(int userId, int numberOfRecommendations, int algorithm);

    List<RecommendedMovieView> getLenskitRecommendations(int userId, int numberOfRecommendations, int algorithm);

    Image[] getImagesFromMovieULRs(int nrImages, String[] movieLinks);

    void giveRatingToMovieByUser(int userId, int movieId, double rating);

    List<RecommendedMovieView> getRandomMovies(int randomMoviesNumber);

    List<RecommendedMovieView> getAllRatedMovies(int userId);

    void systemInitialize(int userId);

    void createTestSets();
}
