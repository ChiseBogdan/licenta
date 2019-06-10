package com.licenta.service;

import com.licenta.model.Movie;
import org.apache.mahout.cf.taste.recommender.IDRescorer;

import java.util.*;

public class Excluder implements IDRescorer {

    private Set<Long> excludedMoviesIDs = new HashSet<>();

    public void addNewExcludedMovie(Movie movie){
        excludedMoviesIDs.add(movie.getMovieId().longValue());
    }

    public void addNewExcludedMovies(List<Movie> movies){
        for(Movie movie: movies){
            excludedMoviesIDs.add(movie.getMovieId().longValue());
        }
    }

    public Set<Long> getIDsOfExcludedMovies(){
        return excludedMoviesIDs;
    }

    @Override
    public double rescore(long l, double v) {
        return v;
    }

    @Override
    public boolean isFiltered(long key) {

        if(excludedMoviesIDs.contains(key) == true){
            return true;
        }

        return false;
    }
}
