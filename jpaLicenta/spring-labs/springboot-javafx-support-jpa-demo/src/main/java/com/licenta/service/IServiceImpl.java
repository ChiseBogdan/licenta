
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = false)
@RequiredArgsConstructor
public class IServiceImpl implements IService {

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

    private Excluder excluder = new Excluder();

    List<Movie> excludedMovies = new ArrayList<>();


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

    private List<Link> getRecommendedLinksFromRecommendedMovies(List<Movie> movies){

        List<Link> links = new ArrayList<>();

        for(Movie movie: movies){
            Link link = movie.getLink();
            links.add(link);
        }

        return links;

    }

    private List<Movie> getRecommendedMovies(List<RecommendedItem> recommendedItems){

        List<Movie> recommendedMovies = new ArrayList<>();

        for(RecommendedItem recommendedItem: recommendedItems){

            Optional<Movie> movieOptional = movieRepository.findById((int)recommendedItem.getItemID());
            if(movieOptional.isPresent()){
                Movie movie = movieOptional.get();
                movie.setExclude(true);
                excluder.addNewExcludedMovie(movie);
                recommendedMovies.add(movie);
            }

        }

        return recommendedMovies;

    }

    private int numberOfDigits(Long imdbId){
        int s=0;

        while (imdbId >0){
            imdbId /=10;
            s++;
        }

        return s;
    }

    private String[] createRecommendedImagesLinks(List<Link> links){

        String[] recommendedLinks = new String[links.size()];
        String baseURL = "https://www.imdb.com/title/tt";


        for (int i=0; i<links.size(); i++){
            Link link = links.get(i);

            // Zeroes need to be added to have 7 digits after tt
            for(int j=0; j< 7- numberOfDigits(link.getImdbId()); j++){
                baseURL = baseURL + "0";
            }

            String linkURL = baseURL + link.getImdbId();
            recommendedLinks[i] = linkURL;
        }

        return recommendedLinks;

    }

    private Image[] getRecommendedImages(List<Link> links){
        String[] recommendedLinks = createRecommendedImagesLinks(links);
        Image[] recommendedImages = getImagesFromMovieULRs(links.size(), recommendedLinks);

        return recommendedImages;
    }

    @Override
    public List<RecommendedMovieView> getRecommendations(int userId, int numberOfRecommendations, int algorithm){

        excludeAlreadyRatedMovies(userId);

        List<RecommendedItem> rawRecommendations = getMahootRecommendations(userId, numberOfRecommendations, algorithm);
        List<Movie> recommendedMovies = getRecommendedMovies(rawRecommendations);
        List<Link> recommendedLinks = getRecommendedLinksFromRecommendedMovies(recommendedMovies);
        Image[] recommendedImages = getRecommendedImages(recommendedLinks);

        return createRecommendedMovieViews(rawRecommendations, recommendedMovies, recommendedImages);

    }

    private void excludeAlreadyRatedMovies(int userId) {

        Optional<User> userOptional = userRepository.findById(userId);

        if(userOptional.isPresent()){
            User user = userOptional.get();
            List<Rating> ratings = user.getRatings();

            for(Rating rating: ratings){
                Movie movie = rating.getMovie();
                excludedMovies.add(movie);
            }
        }

        excluder.addNewExcludedMovies(excludedMovies);

    }

    private List<RecommendedMovieView> createRecommendedMovieViews(List<RecommendedItem> rawRecommendations, List<Movie> recommendedMovies, Image[] recommendedImages) {

        List<RecommendedMovieView> movieViews = new ArrayList<>();

        for(int i=0; i<rawRecommendations.size(); i++){
            RecommendedItem rawRecommendation = rawRecommendations.get(i);
            Movie movie = recommendedMovies.get(i);

            movieViews.add(new RecommendedMovieView(movie.getTitle(), recommendedImages[i], (long) rawRecommendation.getValue(), movie.getMovieId()));
        }

        return movieViews;

    }


    // algorithm 1 for item based and 2 for user based
    public List<RecommendedItem> getMahootRecommendations(int userId, int numberOfRecommendations, int algorithm) {

        Recommender recommender = null;
        List<RecommendedItem> recommendations = null;

        try {

            DataModel model = getDataModel();

            if(algorithm == 1){

                //item item with euclidean distance
                ItemSimilarity itemSimilarity  = new EuclideanDistanceSimilarity(model);
                recommender = new GenericItemBasedRecommender(model, itemSimilarity);
            }
            else if(algorithm == 2){

                // user user with Pearson
                UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(model);
                UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.8, userSimilarity, model);
                recommender = new GenericUserBasedRecommender(model, neighborhood, userSimilarity);

            }

            recommendations = recommender.recommend(userId, numberOfRecommendations, excluder);

        } catch (TasteException e) {
            e.printStackTrace();
        }

        return recommendations;

    }

    @Override
    public Image[] getImagesFromMovieULRs(int nrImages, String[] movieLinks){
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

        if(movieOptional.isPresent()){

            Optional<User> userOptional = userRepository.findById(userId);

            if(userOptional.isPresent()){
                Movie movie = movieOptional.get();
                User user = userOptional.get();

                movie.addRating(user, rating, 0);
                movieRepository.save(movie);
            }

        }
    }

    public void mahoot() {

        Recommender recommender = null;

        try {

            DataModel model = getDataModel();

//            ItemSimilarity similarity = new CachingItemSimilarity(new EuclideanDistanceSimilarity(model), model);
//            SamplingCandidateItemsStrategy strategy = new SamplingCandidateItemsStrategy(10, 5);
//            recommender = new CachingRecommender(new GenericItemBasedRecommender(model, similarity, strategy, strategy));

            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(3.0, similarity, model);
            recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);


            List<RecommendedItem> recommendations = recommender.recommend(2, 10);
            for (RecommendedItem recommendation : recommendations) {
                System.out.println(recommendation);
            }
        } catch (TasteException e) {
            e.printStackTrace();
        }


    }

}
