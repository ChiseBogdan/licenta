
package com.licenta.service;

import com.licenta.model.Link;
import com.licenta.model.Movie;
import com.licenta.model.Rating;
import com.licenta.model.User;
import com.licenta.repository.*;
import com.licenta.ui.views.RecommendedMovieView;
import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.ConnectionPoolDataSource;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.ReloadFromJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.lenskit.LenskitRecommender;
import org.lenskit.LenskitRecommenderEngine;
import org.lenskit.api.ItemRecommender;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.dao.file.StaticDataSource;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.entities.CommonTypes;
import org.lenskit.data.entities.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional(readOnly = false)
@RequiredArgsConstructor
public class IServiceImpl implements IService {

    private LenskitRecommenderEngine itemItemEngine = null;
    private LenskitRecommenderEngine userUserEngine = null;

    private ItemRecommender itemItemLenskitRecommender = null;
    private ItemRecommender userUserLenskitRecommender = null;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    private DataModel mahoutModel = null;

    private Excluder excluder = new Excluder();

    private List<Movie> excludedMovies = new ArrayList<>();

    private Path dataFile = Paths.get("data/movielens.yml");

    private DataAccessObject dao;

//    public void databaseInit() {
//
//        DatabaseInit databaseInit = new DatabaseInit(genreRepository, movieRepository, linkRepository, tagRepository, userRepository);
//
//        String basePath = "/home/bogdan/Desktop/Licenta/ml-latest-small/";
//        String moviePath = basePath + "realMovies.csv";
//        String linksPath = basePath + "links.csv";
//        String tagsPath = basePath + "tags.csv";
//        String ratingsPath = basePath + "ratings.csv";
//
////        databaseInit.test();
//
////        databaseInit.initGenres();
////        databaseInit.initMovies(moviePath);
////        databaseInit.initLinks(linksPath);
////        databaseInit.initTags(tagsPath);
////        databaseInit.initUsers();
////        databaseInit.initRatings(ratingsPath);
//    }

    private DataModel getDataModel() throws TasteException {

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setUser("movie");
        dataSource.setPassword("movie");
        dataSource.setDatabaseName("movies_rec");


        DataModel datamodel = new ReloadFromJDBCDataModel(
                new MySQLJDBCDataModel(new ConnectionPoolDataSource(dataSource), "rating", "user_user_id", "movie_movie_id", "rating", "timestamp"));
        return datamodel;
    }

    private List<Link> getLinksFromMovies(List<Movie> movies) {

        List<Link> links = new ArrayList<>();

        for (Movie movie : movies) {
            Link link = movie.getLink();
            links.add(link);
        }

        return links;

    }

    private List<Movie> getRecommendedMoviesFromMahoutRecommendations(List<RecommendedItem> recommendedItems) {

        List<Movie> recommendedMovies = new ArrayList<>();

        for (RecommendedItem recommendedItem : recommendedItems) {

            Optional<Movie> movieOptional = movieRepository.findById((int) recommendedItem.getItemID());
            if (movieOptional.isPresent()) {
                Movie movie = movieOptional.get();
                movie.setExclude(true);
                excluder.addNewExcludedMovie(movie);
                recommendedMovies.add(movie);
            }

        }

        return recommendedMovies;

    }

    private int numberOfDigits(Long imdbId) {
        int s = 0;

        while (imdbId > 0) {
            imdbId /= 10;
            s++;
        }

        return s;
    }

    private String[] createRecommendedImagesLinks(List<Link> links) {

        String[] recommendedLinks = new String[links.size()];
        String baseURL = "https://www.imdb.com/title/tt";


        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);

            // Zeroes need to be added to have 7 digits after tt
            for (int j = 0; j < 7 - numberOfDigits(link.getImdbId()); j++) {
                baseURL = baseURL + "0";
            }

            String linkURL = baseURL + link.getImdbId();
            recommendedLinks[i] = linkURL;
        }

        return recommendedLinks;

    }

    private Image[] getMovieImages(List<Link> links) {
        String[] recommendedLinks = createRecommendedImagesLinks(links);
        Image[] recommendedImages = getImagesFromMovieULRs(links.size(), recommendedLinks);

        return recommendedImages;
    }

    // sys 1 = Mahout sys 2 = Lenskit
    @Override
    public List<RecommendedMovieView> getMahoutRecommendations(int userId, int numberOfRecommendations, int algorithm) {

        List<RecommendedItem> rawRecommendations = generateMahootRecommendations(userId, numberOfRecommendations, algorithm);
        List<Movie> recommendedMovies = getRecommendedMoviesFromMahoutRecommendations(rawRecommendations);
        List<Link> recommendedLinks = getLinksFromMovies(recommendedMovies);
        Image[] recommendedImages = getMovieImages(recommendedLinks);

        return createRecommendedMovieViewsFromMahout(rawRecommendations, recommendedMovies, recommendedImages);

    }

    public List<Movie> getRecommendedMoviesFromLenskitRecommendations(ResultList recommendations){

        List<Movie> movies = new ArrayList<>();
        int numberOfMovies=0;

        for (Result item : recommendations) {

            int movieId = (int)item.getId();
            Optional<Movie> optionalMovie = movieRepository.findById(movieId);

            if(numberOfMovies == 9) break;

            if(optionalMovie.isPresent()){

                Movie movie = optionalMovie.get();
                movies.add(movie);
                numberOfMovies++;

            }
        }

        excluder.addNewExcludedMovies(movies);

        return movies;

    }

    private List<RecommendedMovieView> createRecommendedMovieViewsFromLenskit(ResultList rawRecommendations, List<Movie> recommendedMovies, Image[] recommendedImages){
        List<RecommendedMovieView> movieViews = new ArrayList<>();

        for (int i = 0; i < rawRecommendations.size() && i<recommendedMovies.size(); i++) {

            Result recommendedMovie = rawRecommendations.get(i);
            Movie movie = recommendedMovies.get(i);

            movieViews.add(new RecommendedMovieView(movie.getTitle(), recommendedImages[i], (long)recommendedMovie.getScore(), movie.getMovieId()));
        }

        return movieViews;
    }

    @Override
    public List<RecommendedMovieView> getLenskitRecommendations(int userId, int numberOfRecommendations, int algorithm) {

        ResultList rawRecommendations = generateLenskitRecommendations(userId, numberOfRecommendations+2, algorithm);
        List<Movie> recommendedMovies = getRecommendedMoviesFromLenskitRecommendations(rawRecommendations);
        List<Link> recommendedLinks = getLinksFromMovies(recommendedMovies);
        Image[] recommendedImages = getMovieImages(recommendedLinks);

        return createRecommendedMovieViewsFromLenskit(rawRecommendations, recommendedMovies, recommendedImages);


    }


    private void initializeLenskitRecommenderEngine() {

        try {
            itemItemEngine = LenskitRecommenderEngine.load(new File("lenskitItemItemEngine"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            userUserEngine = LenskitRecommenderEngine.load(new File("lenskitUserUserEngine"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            StaticDataSource data = StaticDataSource.load(dataFile);
            dao = data.get();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void excludeAlreadyRatedMovies(int userId) {

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<Rating> ratings = user.getRatings();

            for (Rating rating : ratings) {
                Movie movie = rating.getMovie();
                excludedMovies.add(movie);
            }
        }

        excluder.addNewExcludedMovies(excludedMovies);

    }

    private List<RecommendedMovieView> createRandomMovieViews(List<Movie> movies, Image[] movieImages) {

        List<RecommendedMovieView> movieViews = new ArrayList<>();

        int i = 0;

        for (i = 0; i < movies.size(); i++) {

            Movie movie = movies.get(i);
            movieViews.add(new RecommendedMovieView(movie.getTitle(), movieImages[i], Long.valueOf(0), movie.getMovieId()));
        }

        return movieViews;
    }

    private List<RecommendedMovieView> createRecommendedMovieViewsFromMahout(List<RecommendedItem> rawRecommendations, List<Movie> recommendedMovies, Image[] recommendedImages) {

        List<RecommendedMovieView> movieViews = new ArrayList<>();

        for (int i = 0; i < rawRecommendations.size() && i< recommendedMovies.size(); i++) {
            RecommendedItem rawRecommendation = rawRecommendations.get(i);
            Movie movie = recommendedMovies.get(i);

            movieViews.add(new RecommendedMovieView(movie.getTitle(), recommendedImages[i], (long) rawRecommendation.getValue(), movie.getMovieId()));
        }

        return movieViews;

    }

    // algorithm 1 for item based and 2 for user based
    public ResultList generateLenskitRecommendations(int userId, int numberOfRecommendatinos, int algorithm){

        ResultList recommendations = null;

        if(algorithm == 1){

            try (LenskitRecommender recItemItem = itemItemEngine.createRecommender(dao)){

                itemItemLenskitRecommender = recItemItem.getItemRecommender();
                recommendations = itemItemLenskitRecommender.recommendWithDetails(userId, numberOfRecommendatinos, null, excluder.getIDsOfExcludedMovies());

            }
        }
        else if(algorithm == 2){

            try (LenskitRecommender recUserUser = userUserEngine.createRecommender(dao);){

                userUserLenskitRecommender = recUserUser.getItemRecommender();
                recommendations = userUserLenskitRecommender.recommendWithDetails(userId, numberOfRecommendatinos, null, excluder.getIDsOfExcludedMovies());

            }
        }

        return recommendations;

    }


    // algorithm 1 for item based and 2 for user based
    public List<RecommendedItem> generateMahootRecommendations(int userId, int numberOfRecommendations, int algorithm) {

        Recommender recommender = null;
        List<RecommendedItem> recommendations = null;

        try {

            if (algorithm == 1) {

                //item item with euclidean distance
                ItemSimilarity itemSimilarity = new EuclideanDistanceSimilarity(mahoutModel);
                recommender = new GenericItemBasedRecommender(mahoutModel, itemSimilarity);
            } else if (algorithm == 2) {

                // user user with Pearson
                UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(mahoutModel);
                UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.8, userSimilarity, mahoutModel);
                recommender = new GenericUserBasedRecommender(mahoutModel, neighborhood, userSimilarity);

            }

            recommendations = recommender.recommend(userId, numberOfRecommendations, excluder);

        } catch (TasteException e) {
            e.printStackTrace();
        }

        return recommendations;

    }

    @Override
    public Image[] getImagesFromMovieULRs(int nrImages, String[] movieLinks) {
        Image[] images = new Image[nrImages + 1];
        Thread[] threads = new Thread[nrImages];

//        String movieLink = "https://www.imdb.com/title/tt0114709/";

        for (int i = 0; i < nrImages; i++) {
            GetMovieImagesThread t = new GetMovieImagesThread(images, movieLinks[i], i);
            threads[i] = t;
            t.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return images;
    }

    @Override
    public void giveRatingToMovieByUser(int userId, int movieId, double rating) {

        Optional<Movie> movieOptional = movieRepository.findById(movieId);

        if (movieOptional.isPresent()) {

            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isPresent()) {
                Movie movie = movieOptional.get();
                User user = userOptional.get();

                movie.addRating(user, rating, 0);
                movieRepository.save(movie);

                excludedMovies.add(movie);
                excluder.addNewExcludedMovie(movie);
            }

        }
    }

    private List<Movie> findRandomMovies(int randomMoviesNumber) {

        List<Movie> movies = new ArrayList<>();

        Random random = new Random();
        int i = 0;

        while (i < randomMoviesNumber) {

            int randomId = random.nextInt(9700);

            Optional<Movie> movieOptional = movieRepository.findById(randomId);
            if (movieOptional.isPresent()) {
                movies.add(movieOptional.get());
                i++;
            }
        }

        return movies;

    }

    @Override
    public List<RecommendedMovieView> getRandomMovies(int randomMoviesNumber) {

        List<Movie> randomMovies = findRandomMovies(randomMoviesNumber);
        List<Link> randomMovieLinks = getLinksFromMovies(randomMovies);
        Image[] randomMovieImages = getMovieImages(randomMovieLinks);

        return createRandomMovieViews(randomMovies, randomMovieImages);

    }

    private List<Rating> getAllRatingsFromAUser(int userId) {

        Optional<User> optionalUser = userRepository.findById(userId);

        List<Rating> allRatingsFromAUser = null;

        if (optionalUser.isPresent()) {

            User user = optionalUser.get();
            allRatingsFromAUser = user.getRatings();

        }

        return allRatingsFromAUser;

    }

    private List<RecommendedMovieView> createRatedMoviesViews(List<Rating> ratings, List<Movie> allRatedMovies, Image[] allRatedMovieImages) {

        List<RecommendedMovieView> ratedMovieViews = new ArrayList<>();

        int i;

        for (i = 0; i < ratings.size(); i++) {

            Rating rating = ratings.get(i);
            Movie movie = allRatedMovies.get(i);

            if (movie.getMovieId() == rating.getMovie().getMovieId()) {
                ratedMovieViews.add(new RecommendedMovieView(movie.getTitle(), allRatedMovieImages[i], rating.getRating().longValue(), movie.getMovieId()));
            }

        }

        return ratedMovieViews;

    }

    private List<Movie> getAllRatedMoviesFromRatings(List<Rating> ratings) {

        List<Movie> ratedMovies = new ArrayList<>();

        for (Rating rating : ratings) {
            ratedMovies.add(rating.getMovie());
        }

        return ratedMovies;

    }

    @Override
    public List<RecommendedMovieView> getAllRatedMovies(int userId) {

        List<Rating> allRatingsFromAUser = getAllRatingsFromAUser(userId);
        List<Movie> allRatedMovies = getAllRatedMoviesFromRatings(allRatingsFromAUser);
        Image[] allRatedMovieImages = getMovieImages(getLinksFromMovies(allRatedMovies));

        return createRatedMoviesViews(allRatingsFromAUser, allRatedMovies, allRatedMovieImages);
    }

    @Override
    public void systemInitialize(int userId) {

        try {
            mahoutModel = getDataModel();
        } catch (TasteException e) {
            e.printStackTrace();
        }

        initializeLenskitRecommenderEngine();
        excludeAlreadyRatedMovies(userId);

    }

    private void firstSetLenskit(){

        Recommender recommender = null;

        try {

            try {
                mahoutModel = getDataModel();
            } catch (TasteException e) {
                e.printStackTrace();
            }

            //item item with euclidean distance
            ItemSimilarity itemSimilarity = new EuclideanDistanceSimilarity(mahoutModel);
            recommender = new GenericItemBasedRecommender(mahoutModel, itemSimilarity);

            //userId item
            float rating = recommender.estimatePreference(1,2);

            System.out.println("sss");

        } catch (TasteException e) {
            e.printStackTrace();
        }

//        int randomMoviesNumber = 5000;
//
//        List<Movie> randomMovies = findRandomMovies(randomMoviesNumber);

    }

    @Override
    public void createTestSets() {

        firstSetLenskit();

    }

}
