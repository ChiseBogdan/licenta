package com.licenta.service;

import com.licenta.model.Movie;
import org.apache.mahout.cf.taste.recommender.IDRescorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Excluder implements IDRescorer {

    private Map<Long, Movie> excludedMovies = new HashMap<>();

    public void addNewExcludedMovie(Movie movie){
        excludedMovies.put((long)movie.getMovieId(), movie);
    }

    public void addNewExcludedMovies(List<Movie> movies){
        for(Movie movie: movies){
            excludedMovies.put((long)movie.getMovieId(), movie);
        }
    }

    @Override
    public double rescore(long l, double v) {
        return v;
    }

    @Override
    public boolean isFiltered(long key) {

        if(excludedMovies.containsKey(key) == true){
            return true;
        }

        return false;
    }
}
