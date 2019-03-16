package com.licenta.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "genre_id")
    private Integer genreId;

    @Enumerated(EnumType.STRING)
    private GenreEnum genreType;

    @ManyToMany(mappedBy = "genres")
    private List<Movie> movies = new ArrayList<>();

    public Genre(GenreEnum genreType){
        this.genreType = genreType;
    }

    public Genre(){}

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public GenreEnum getGenreType() {
        return genreType;
    }

    public void setGenreType(GenreEnum genreType) {
        this.genreType = genreType;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }
}
