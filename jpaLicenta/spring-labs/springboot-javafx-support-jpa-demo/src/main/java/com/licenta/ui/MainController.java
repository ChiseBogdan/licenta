package com.licenta.ui;

import com.licenta.service.GetMovieImagesThread;
import com.licenta.ui.views.RecommendedMovieView;
import de.felixroske.jfxsupport.FXMLController;
import com.licenta.service.IService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.controlsfx.control.Rating;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FXMLController
public class MainController {


    @Autowired
    private IService service;

    @FXML
    private RadioButton userUserRadioButton;

    @FXML
    private RadioButton itemItemRadioButton;

    @FXML
    private FlowPane moviesPane;

    @FXML
    private ScrollPane movieScrollPane;

    final ToggleGroup group = new ToggleGroup();

    private int userId = 2;
    private int numberOfRecommendations = 9;
    private int recommendationsAlgorithm = 1;

    @FXML
    public void initialize() {

        initialAppearances();

        loadMovies();

        movieScrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {

                    if(newValue.doubleValue() == movieScrollPane.getVmax()){
                        loadMovies();
                    }
                });

        initializeRadioButtonGroup();

    }

    private void initializeRadioButtonGroup() {

        userUserRadioButton.setToggleGroup(group);
        userUserRadioButton.setSelected(true);

        itemItemRadioButton.setToggleGroup(group);

        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
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

    private void loadMovies() {

        List<RecommendedMovieView> recommendedMovieViews = service.getRecommendations(userId, numberOfRecommendations, recommendationsAlgorithm);

        createMovieImagesFlowPane(recommendedMovieViews, recommendedMovieViews.size());
    }

    private void initialAppearances(){

        moviesPane.setLayoutX(50);

        moviesPane.setHgap(10);
        moviesPane.setVgap(20);
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

    private void createMovieImagesFlowPane(List<RecommendedMovieView> recommendedMovieViews, int nrImages) {

        for (int i = 0; i < nrImages; i++) {

            RecommendedMovieView movieView = recommendedMovieViews.get(i);

            Pane pane = new Pane();

            ImageView imageView = new ImageView();
            imageView.setImage(movieView.getImage());
            imageView.setX(75);

            Rating rating = new Rating(5);
            rating.setId(String.valueOf( movieView.getMovieId()));
            rating.setPartialRating(true);
            rating.setRating(movieView.getRatingValue());

            setRatingAspect(rating);

            pane.getChildren().add(imageView);
            pane.getChildren().add(rating);

            moviesPane.getChildren().add(pane);

        }
    }
}
