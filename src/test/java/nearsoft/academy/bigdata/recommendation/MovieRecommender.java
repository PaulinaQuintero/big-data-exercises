package nearsoft.academy.bigdata.recommendation;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.io.File;
import java.io.IOException;

public class MovieRecommender {


    private final GenericUserBasedRecommender recommender;
    private HashBiMap<String, Integer> pr;
    private HashBiMap<String, Integer> us;
    private int totalReviews=0;

    public MovieRecommender(String files) throws IOException, TasteException {
        System.out.println("Static method");
        DataModel model = new FileDataModel(new File(transCSV(files)));
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
        recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
    }

    private String transCSV(String files) {
        try {

            File original = new File(files);
            pr= HashBiMap.create();
            us= HashBiMap.create ();
            File como = new File(original.getParentFile().getAbsolutePath() + "/dataset.csv");
            if (como.exists()) {
                como.delete();
            }
            else{
                como.createNewFile();
            }

            try
                    (InputStream fileStream = new FileInputStream(files);
                    InputStream gzipStream = new GZIPInputStream(fileStream);
                    Reader read = new InputStreamReader(gzipStream, "UTF8");
                    BufferedReader br = new BufferedReader(read);
                    Writer writer = new BufferedWriter(new FileWriter(como));)
            {
                String score = "";
                String line;
                Integer userId = null;
                Integer productId = null;
                boolean status = false;
                while ((line = br.readLine()) != null) {
                    if (status) {
                        if (line.contains("review/userId")) {
                            String user =getValuePortionOfString(line);
                            if(!us.containsKey(user)){
                                us.put(user,us.size()+1);
                            }
                            userId = us.get(user);

                        } else if (line.contains("review/score")) {
                            score = getValuePortionOfString(line);
                        } else if (line.contains("review/summary")) {
                            writer.append(String.valueOf(userId));
                            writer.append(",");
                            writer.append(String.valueOf(productId));
                            writer.append(",");
                            writer.append(score);
                            writer.append("\n");
                            score = "";
                            productId = null;
                            status = false;
                            userId = null;
                        }
                    } else if (line.contains("product/productId")) {
                        String productName = getValuePortionOfString(line);
                        if(!pr.containsKey(productName)){
                            pr.put(productName,pr.size()+1);
                        }
                        productId = pr.get(productName);
                        status = true;
                        totalReviews++;
                    }

                }
            }
            return como.getAbsolutePath();
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(files, e);
        }
    }

    private String getValuePortionOfString(String line) {
        return line.substring(line.indexOf(":") + 2, line.length());
    }
    public long getTotalReviews() {
        return totalReviews;
    }
    public long getTotalProducts(){
        return pr.size();
    }
    public long getTotalUsers(){
        return us.size();
    }
    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<String> getRecommendationsForUser(String user) throws TasteException {
        List<RecommendedItem> recommendations = recommender.recommend(us.get(user), 3);
        List<String> recommendMovie = new ArrayList<>();

        BiMap<Integer, String> products = pr.inverse();
        for (RecommendedItem recommendation : recommendations) {
            System.out.println(recommendation);
            String movie = products.get((int)recommendation.getItemID());
            recommendMovie.add(movie);
        }
        return recommendMovie;
    }
}