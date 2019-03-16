
package com.licenta.service;

import com.licenta.repository.*;
import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.ConnectionPoolDataSource;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.ReloadFromJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.SamplingCandidateItemsStrategy;
import org.apache.mahout.cf.taste.impl.similarity.CachingItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.JDBCDataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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


    public void initalize() {

        DatabaseInit databaseInit = new DatabaseInit(genreRepository, movieRepository, linkRepository, tagRepository, userRepository);

        String basePath = "/home/bogdan/Desktop/Licenta/ml-latest-small/";
        String moviePath = basePath + "realMovies.csv";
        String linksPath = basePath + "links.csv";
        String tagsPath = basePath + "tags.csv";
        String ratingsPath = basePath + "ratings.csv";

//        databaseInit.test();

//        databaseInit.initGenres();
//        databaseInit.initMovies(moviePath);
//        databaseInit.initLinks(linksPath);
//        databaseInit.initTags(tagsPath);
//        databaseInit.initUsers();
//        databaseInit.initRatings(ratingsPath);
    }

    DataModel getDataModel() throws TasteException {

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setUser("movie");
        dataSource.setPassword("movie");
        dataSource.setDatabaseName("movies_rec");


        DataModel datamodel = new ReloadFromJDBCDataModel(
                new MySQLJDBCDataModel(new ConnectionPoolDataSource(dataSource), "rating", "user_user_id", "movie_movie_id", "rating", "timestamp"));
        return datamodel;
    }

    @Override
    public void mahoot() {

        Recommender recommender = null;

        try {

            DataModel model = getDataModel();

//            ItemSimilarity similarity = new CachingItemSimilarity(new EuclideanDistanceSimilarity(model), model);
//            SamplingCandidateItemsStrategy strategy = new SamplingCandidateItemsStrategy(10, 5);
//            recommender = new CachingRecommender(new GenericItemBasedRecommender(model, similarity, strategy, strategy));

            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
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
