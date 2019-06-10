package com.licenta.ui;

import com.licenta.service.GetMovieImagesThread;
import com.licenta.ui.views.RecommendedMovieView;
import de.felixroske.jfxsupport.FXMLController;
import com.licenta.service.IService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.controlsfx.control.Rating;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@FXMLController
public class MainController {


    @Autowired
    private IService service;

    @FXML
    private RadioButton mahoutEngine;

    @FXML
    private RadioButton lenskitEngine;

    @FXML
    private RadioButton userUserRadioButton;

    @FXML
    private RadioButton itemItemRadioButton;

    @FXML
    private ScrollPane recommendationsScrollPane;

    @FXML
    private FlowPane recommendataionsPane;

    @FXML
    private ScrollPane browsingMoviesScrollPane;

    @FXML
    private FlowPane browsingMoviesPane;

    @FXML
    private ScrollPane myRatingScrollPane;

    @FXML
    private FlowPane myRatingFlowPane;

    final ToggleGroup algorithGroup = new ToggleGroup();
    final ToggleGroup engineGroup = new ToggleGroup();

    private int userId = 2;
    private int numberOfRecommendations = 9;
    private int recommendationsAlgorithm = 1;
    private int recommendationEngine =1;

    @FXML
    public void initialize() {

//        service.createTestSets();

        service.systemInitialize(userId);

        initialAppearances();

        initialLoadOfMovies();

        setInfiniteScrolling();

    }

    private void setInfiniteScrolling() {
        setInfiniteScrollingForRecommendations();
        setInfiniteScrollingForBrowsing();
    }

    private void setInfiniteScrollingForBrowsing() {

        browsingMoviesScrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {

                    if(newValue.doubleValue() == browsingMoviesScrollPane.getVmax()){
                        loadBrowsingMovies();
                    }
                });
    }

    private void setInfiniteScrollingForRecommendations() {
        recommendationsScrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {

                    if(newValue.doubleValue() == recommendationsScrollPane.getVmax()){
                        loadRecommendationsMovies();
                    }
                });

        initializeRadioButtonGroups();
    }

    private void initializeRadioButtonGroups() {

        initializeRadioButtonAlgorithmGroup();
        initializeRadioButtonEngineGroup();
    }

    private void initializeRadioButtonEngineGroup() {

        mahoutEngine.setToggleGroup(engineGroup);
        mahoutEngine.setSelected(true);

        lenskitEngine.setToggleGroup(engineGroup);

        engineGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                if (mahoutEngine.isSelected()) {
                    recommendationEngine = 1;
                }
                else{
                    recommendationEngine = 2;
                }
            }
        });
    }

    private void initializeRadioButtonAlgorithmGroup() {

        userUserRadioButton.setToggleGroup(algorithGroup);
        userUserRadioButton.setSelected(true);

        itemItemRadioButton.setToggleGroup(algorithGroup);

        algorithGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                if (itemItemRadioButton.isSelected()) {
                    recommendationsAlgorithm = 1;
                }
                else{
                    recommendationsAlgorithm = 2;
                }
            }
        });
    }

    private void loadRecommendationsMovies(){

        if(recommendationEngine == 1){
            List<RecommendedMovieView> recommendedMovieViews = service.getMahoutRecommendations(userId, numberOfRecommendations, recommendationsAlgorithm);

            createMovieImagesFlowPane(recommendedMovieViews, recommendedMovieViews.size(), recommendataionsPane);
        }
        else if(recommendationEngine == 2){
            List<RecommendedMovieView> recommendedMovieViews = service.getLenskitRecommendations(userId, numberOfRecommendations, recommendationsAlgorithm);

            createMovieImagesFlowPane(recommendedMovieViews, recommendedMovieViews.size(), recommendataionsPane);
        }


    }

    private void loadBrowsingMovies(){
        int randomMoviesNumber = 4;
        List<RecommendedMovieView> recommendedMovieViews = service.getMahoutRecommendations(userId, numberOfRecommendations-randomMoviesNumber, recommendationsAlgorithm);
        List<RecommendedMovieView> randomMoviesViews = service.getRandomMovies(randomMoviesNumber);

        setRatingToZeroForRecommendedMoviesInBrowsing(recommendedMovieViews);

        List<RecommendedMovieView> allBrowsingMovieViews = new ArrayList<>();
        allBrowsingMovieViews.addAll(recommendedMovieViews);
        allBrowsingMovieViews.addAll(randomMoviesViews);

        createMovieImagesFlowPane(allBrowsingMovieViews, allBrowsingMovieViews.size(), browsingMoviesPane);
    }

    private void setRatingToZeroForRecommendedMoviesInBrowsing(List<RecommendedMovieView> recommendedMovieViews) {

        for(RecommendedMovieView movieView: recommendedMovieViews){
            movieView.setRatingValue(Long.valueOf(0));
        }
    }

    private void initialLoadOfMovies() {

        loadRecommendationsMovies();
        loadBrowsingMovies();
        loadRatedMovies();
    }

    private void loadRatedMovies() {

        List<RecommendedMovieView> allRatedMovies = service.getAllRatedMovies(userId);

        createMovieImagesFlowPane(allRatedMovies, allRatedMovies.size(), myRatingFlowPane);
    }

    private void initialAppearances(){

        setRecommendationsMoviesPane();
        setBrowsingMoviesPane();
    }

    private void setBrowsingMoviesPane() {

        browsingMoviesPane.setLayoutX(50);

        browsingMoviesPane.setHgap(10);
        browsingMoviesPane.setVgap(20);
    }

    private void setRecommendationsMoviesPane() {

        recommendataionsPane.setLayoutX(50);

        recommendataionsPane.setHgap(10);
        recommendataionsPane.setVgap(20);
    }

    private void setRatingAspect(Rating rating){
        double lowerThreshold = 0.35;
        double upperThresold = 0.6;

        rating.ratingProperty().addListener((obs, old, newValue) -> {

            int integerNewValue = newValue.intValue();
            double newValueDoubleValue = newValue.doubleValue();
            double finalModifiedValue = 0;

            if (newValueDoubleValue - 0.5 != integerNewValue && integerNewValue != newValueDoubleValue) {

                if (newValueDoubleValue > integerNewValue + lowerThreshold && newValueDoubleValue < integerNewValue + upperThresold) {
                    finalModifiedValue = integerNewValue + 0.5;
                } else if (newValueDoubleValue <= integerNewValue + lowerThreshold) {
                    finalModifiedValue = integerNewValue;
                } else if (newValueDoubleValue >= integerNewValue + upperThresold) {
                    finalModifiedValue = integerNewValue + 1;
                }

                rating.setRating(finalModifiedValue);
                service.giveRatingToMovieByUser(userId, Integer.parseInt(rating.getId()),finalModifiedValue);

            }

        });

        //rating layout setup
        rating.setLayoutX(45);
        rating.setLayoutY(200);
    }

    private void createMovieImagesFlowPane(List<RecommendedMovieView> recommendedMovieViews, int nrImages, FlowPane usedPane) {

        for (int i = 0; i < nrImages; i++) {

            RecommendedMovieView movieView = recommendedMovieViews.get(i);

            Pane pane = new Pane();

            ImageView imageView = new ImageView();
            imageView.setImage(movieView.getImage());
            imageView.setX(55);

            Pane movieTitlePane = new Pane();

            Text movieTitle = new Text();
            movieTitle.setWrappingWidth(180);
            movieTitle.setText(movieView.getTitle());
            movieTitle.setX(55);
            movieTitlePane.setLayoutY(215);
            movieTitlePane.getChildren().add(movieTitle);

            Rating rating = new Rating(5);
            rating.setId(String.valueOf( movieView.getMovieId()));
            rating.setPartialRating(true);
            rating.setRating(movieView.getRatingValue());
            rating.setPadding(new Insets(35, 0, 5, 0));
            setRatingAspect(rating);

            pane.getChildren().add(movieTitlePane);
            pane.getChildren().add(imageView);
            pane.getChildren().add(rating);


            usedPane.getChildren().add(pane);

        }
    }
}
