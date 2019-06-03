package com.licenta.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    public User(){}

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.MERGE
    )
    private List<Rating> ratings = new ArrayList<>();

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void updateRating(int movieId, double ratingScore){

        List<Rating> ratings = this.getRatings();

        for(Rating rating: ratings){
            int gatheredMovieId = rating.getMovie().getMovieId();
            if(gatheredMovieId == movieId){
                rating.setRating(ratingScore);
            }
        }

    }

}
