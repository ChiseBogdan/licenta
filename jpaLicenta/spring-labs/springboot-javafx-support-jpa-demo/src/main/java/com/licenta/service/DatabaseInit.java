package com.licenta.service;

import com.licenta.model.*;
import com.licenta.repository.*;
import jdk.nashorn.internal.runtime.options.Option;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Optional;

public class DatabaseInit {

    private GenreRepository genreRepository;
    private MovieRepository movieRepository;
    private LinkRepository linkRepository;
    private TagRepository tagRepository;
    private UserRepository userRepository;

    public DatabaseInit(GenreRepository genreRepository, MovieRepository movieRepository, LinkRepository linkRepository, TagRepository tagRepository, UserRepository userRepository) {
        this.genreRepository = genreRepository;
        this.movieRepository = movieRepository;
        this.linkRepository = linkRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    // order of exection:
    // 1. initGenres
    // 2. initMovies
    // 3. initLinks
    // 4. initTags
    // 5. initUsers
    // 6. initRatings

    private BufferedReader initBufferReader(String fullPath) {
        File file = new File(fullPath);
        FileReader fr = null;

        BufferedReader br = null;

        try {

            fr = new FileReader(file);
            br = new BufferedReader(fr);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return br;
    }

    private void addLinkToMovie(Integer movieId, Link link) {

        Optional<Movie> movieOptional = movieRepository.findById(movieId);

        if (movieOptional.isPresent()) {
            Movie movie = movieOptional.get();
            movie.setLink(link);
            movieRepository.save(movie);
        }
    }


    public void initLinks(String fullPathToLinks) {

        BufferedReader br = initBufferReader(fullPathToLinks);

        try {
            String line;
            while ((line = br.readLine()) != null) {

                String parts[] = line.split(",");

                if (2 < parts.length) {

                    Integer id = Integer.parseInt(parts[0]);
                    Long imdbId = Long.parseLong(parts[1]);
                    Long tmdbId = Long.parseLong(parts[2]);

                    Link link = new Link(id, imdbId, tmdbId);

                    linkRepository.saveAndFlush(link);

                    addLinkToMovie(id, link);

                } else if (1 < parts.length) {

                    Integer id = Integer.parseInt(parts[0]);
                    Long imdbId = Long.parseLong(parts[1]);
                    Long tmdbId = null;

                    Link link = new Link(id, imdbId, tmdbId);

                    linkRepository.saveAndFlush(link);

                    addLinkToMovie(id, link);


                }


            }

            System.out.println("DONE with Links");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private GenreEnum checkGenreType(String genre) {

        switch (genre) {
            case "Action":
                return GenreEnum.ACTION;
            case "Adventure":
                return GenreEnum.ADVENTURE;
            case "Animation":
                return GenreEnum.ANIMATION;
            case "Children's":
                return GenreEnum.CHILDREN;
            case "Comedy":
                return GenreEnum.COMEDY;
            case "Crime":
                return GenreEnum.CRIME;
            case "Documentary":
                return GenreEnum.DOCUMENTARY;
            case "Drama":
                return GenreEnum.DRAMA;
            case "Fantasy":
                return GenreEnum.FANTASY;
            case "Film-Noir":
                return GenreEnum.FILM_NOIR;
            case "Horror":
                return GenreEnum.HORROR;
            case "Musical":
                return GenreEnum.MUSICAL;
            case "Mystery":
                return GenreEnum.MYSTERY;
            case "Romance":
                return GenreEnum.ROMANCE;
            case "Sci-Fi":
                return GenreEnum.SCI_FI;
            case "Thriller":
                return GenreEnum.THRILLER;
            case "War":
                return GenreEnum.WAR;
            case "Western":
                return GenreEnum.WESTERN;
            default:
                return null;
        }

    }

    public void initMovies(String fullPathToMovies) {

        BufferedReader br = initBufferReader(fullPathToMovies);

        try {

            String line;
            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                String id = null;
                String genres = null;
                String movieTitle = null;
                String year = null;
                Integer intYear = null;

                if (0 < parts.length) {
                    id = parts[0];
                }

                if (1 < parts.length) {
                    movieTitle = parts[1];

                    year = StringUtils.substringBetween(movieTitle, "(", ")");
                    if (year != null && year.matches("[0-9]+")) {
                        intYear = Integer.parseInt(year);
                    }

                    if (year != null && !year.matches("[0-9]+")) {
                        System.out.println("Problem with the movie with id: " + id);
                    }

                }

                Movie movie = new Movie(Integer.parseInt(id), movieTitle, intYear);

                if (2 < parts.length) {
                    genres = parts[2];
                }


                if (genres != null) {

                    if (!genres.contains("|")) {

                        GenreEnum genreEnum = null;
                        genreEnum = checkGenreType(genres);

                        if (genreEnum != null) {
                            Genre genre = new Genre(genreEnum);

                            movie.addGenre(genre);

                        }
                    } else {

                        String genresParts[] = genres.split("\\|");

                        for (int i = 0; i < genresParts.length; i++) {

                            GenreEnum genreEnum = null;
                            genreEnum = checkGenreType(genresParts[i]);

                            if (genreEnum != null) {
                                movie.addGenre(new Genre(genreEnum));

                            }
                        }
                    }

                }

                movieRepository.save(movie);
            }

            System.out.println("DONE");
            movieRepository.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void initRatings(String ratingsPath) {

        BufferedReader br = initBufferReader(ratingsPath);

        try {

            String line;

            while ((line = br.readLine()) != null) {

                String parts[] = line.split(",");

                String userId = null;
                String movieId = null;
                String rating = null;
                String timestamp = null;

                userId = parts[0];
                movieId = parts[1];
                rating = parts[2];
                timestamp = parts[3];

                Integer userIdInt = Integer.parseInt(userId);
                Integer movieIdInt = Integer.parseInt(movieId);
                Double score = Double.parseDouble(rating);
                Long timestampLong = Long.parseLong(timestamp);

                Optional<User> userOptional = userRepository.findById(userIdInt);
                Optional<Movie> movieOptional = movieRepository.findById(movieIdInt);

                if (movieOptional.isPresent() && userOptional.isPresent()) {

                    Movie movie = movieOptional.get();
                    User user = userOptional.get();

                    movie.addRating(user, score, timestampLong);

                    movieRepository.save(movie);
                }

            }
            movieRepository.flush();

            System.out.println("DONE with Ratings");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void initGenres() {

        for (GenreEnum genreEnum : GenreEnum.values()) {

            Genre genre = new Genre(genreEnum);

            genreRepository.save(genre);

        }

    }

    public void initUsers() {

        for (int i = 1; i <= 610; i++) {
            userRepository.save(new User());
        }

        userRepository.flush();
        System.out.println("Done with Users");

    }

    public void initTags(String tagsPath) {

        BufferedReader br = initBufferReader(tagsPath);

        try {

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                String userId = null;
                String movieId = null;
                String tagString = null;
                String timestamp = null;

                userId = parts[0];
                movieId = parts[1];
                tagString = parts[2];
                timestamp = parts[3];

                Integer userIdInt = Integer.parseInt(userId);
                Integer movieIdInt = Integer.parseInt(movieId);
                Long timestampLong = Long.parseLong(timestamp);

                Tag tag = new Tag(userIdInt, tagString, timestampLong);

                Tag returnedTag = tagRepository.saveAndFlush(tag);

                Optional<Movie> movieOptional = movieRepository.findById(movieIdInt);

                if (movieOptional.isPresent()) {
                    Movie movie = movieOptional.get();
                    movie.addTag(returnedTag);
                }

            }

            System.out.println("DONE with Tags");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void test() {

        User user = userRepository.saveAndFlush(new User());

        System.out.println("Here");

    }
}
