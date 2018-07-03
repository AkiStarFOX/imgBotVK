import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.account.Info;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoXtrRealOffset;
import com.vk.api.sdk.objects.photos.responses.GetAllResponse;
import com.vk.api.sdk.objects.photos.responses.GetResponse;
import org.w3c.dom.css.RGBColor;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.List;

public class Main {
    private final static String PROPERTIES_FILE = "config.properties";
    private static final String AUTH_URL = "https://oauth.vk.com/authorize"
            + "?client_id={APP_ID}"
            + "&scope={PERMISSIONS}"
            + "&redirect_uri={REDIRECT_URI}"
            + "&display={DISPLAY}"
            + "&response_type=token";
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/imgdb?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String MYSQL_LOGIN = "root";
    private static final String MYSQL_PASSWORD = "root";
    private static int offset = 0;
    private static ArrayList<String> idList = new ArrayList<>();


    public static void main(String[] args) throws FileNotFoundException, ClientException, ApiException, SQLException {
        Properties properties = readProperties();
        HttpTransportClient client = new HttpTransportClient();
        VkApiClient apiClient = new VkApiClient(client);
        UserActor actor = initVkApi(apiClient, properties);
        Connection connection = DriverManager.getConnection(MYSQL_URL, MYSQL_LOGIN, MYSQL_PASSWORD);
        Statement statement = connection.createStatement();


//      Получение токена
//        try {
//            auth("6502666");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        for(int i=0;i<16;i++){
//            statement.execute("ALTER TABLE imageshsv ADD COLUMN Black DECIMAL(8,6) NOT NULL;");
//            statement.execute("ALTER TABLE imageshsv ADD COLUMN Grey DECIMAL(8,6) NOT NULL;");
//            statement.execute("ALTER TABLE imageshsv ADD COLUMN White DECIMAL(8,6) NOT NULL;");
//            statement.execute("ALTER TABLE imageshsv ADD COLUMN LG DECIMAL(8,6) NOT NULL;");
//        }
//        for (int i = 0; i < 16; i++) {
//            statement.execute("ALTER TABLE imageshsv ADD COLUMN O" + i + " DECIMAL(8,6) NOT NULL;");
//
//        }


        while (true) {
            List<Photo> list = getPhotoFromVK(offset, apiClient, actor);
            HashMap<Integer, HSV> map = downloadImg(list);


            try {

                for (Map.Entry<Integer, HSV> m : map.entrySet()) {
//                    statement.addBatch(sqlAddWithHisto(m.getKey(),m.getValue().URL,m.getValue().getHisto().getH(),m.getValue().getHisto().getS(),m.getValue().getHisto().getV()));

//                    statement.addBatch(sqlAdd(m.getKey(), m.getValue().getHslArray(), m.getValue().getURL()));
                    PixelReader px = m.getValue().getPixelReader();
//                    System.out.println(sqlAddWithHisto(m.getKey(),m.getValue().getURL(),px.getResultMap(),px.getBlack(),px.getGrey(),px.getWhite(),px.getLigth_grey()));
                    statement.addBatch(sqlAddWithHisto(m.getKey(), m.getValue().getURL(), px.getResultMap(), px.getBlack(), px.getGrey(), px.getWhite(), px.getLigth_grey()));
                }
                statement.executeBatch();
                statement.clearBatch();

            } catch (SQLException e) {
                System.out.println("ERROR" + e.getMessage());
            }
            offset += 100;

        }


    }

    public static List<Photo> getPhotoFromVK(int offset, VkApiClient vk, UserActor actor) {
        GetResponse getAllResponse = null;
        try {
            getAllResponse = vk.photos().get(actor)
                    .ownerId(-104375368)
                    .albumId("wall")
                    .rev(false)
                    .count(100)
                    .offset(offset)
                    .execute();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return getAllResponse.getItems();
    }


    private static UserActor initVkApi(VkApiClient apiClient, Properties properties) {
        int groupId = Integer.parseInt(properties.getProperty("groupId"));
        String token = properties.getProperty("token");
        if (groupId == 0 || token == null) throw new RuntimeException("Params are not set");
        UserActor actor = new UserActor(groupId, token);


        return actor;
    }

    private static Properties readProperties() throws FileNotFoundException {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (inputStream == null)
            throw new FileNotFoundException("property file '" + PROPERTIES_FILE + "' not found in the classpath");

        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Incorrect properties file");
        }
    }

    public static HashMap<Integer, HSV> downloadImg(List<Photo> list) {
        File file = null;
        BufferedImage img = null;
        boolean isLoaded = true;
        boolean is1280 = false;
        HashMap<Integer, HSV> hashMap = new HashMap<Integer, HSV>();
        try {

            for (int i = 0; i < list.size(); i++) {
                String fileName = "google" + i + ".png";


                if (list.get(i).getPhoto1280() != null) {

                    img = ImageIO.read(new URL(list.get(i).getPhoto604()));

//

                    if (isLoaded) {
                        file = new File(fileName);
                        if (!file.exists()) {
                            file.createNewFile();

                        }

//                        ImageIO.write(img, "png", file);
//                        int[][] array = ColorThief.getPalette(img, 5, 1, false);
//                        Histo histo = new Histo(img);
                        PixelReader px = new PixelReader(img);
                        System.out.println(list.get(i).getPhoto604());



                        hashMap.put(list.get(i).getId(), new HSV(px, list.get(i).getPhoto1280()));


                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return hashMap;
    }

    public static String sqlAdd(Integer key, float[][] array, String URL) {
        StringBuilder s = new StringBuilder("INSERT IGNORE INTO imagesHSV (Img_id,URL,H1,S1,V1,H2,S2,V2,H3,S3,V3,H4,S4,V4,H5,S5,V5) VALUES ('" + key + "','" + URL + "'");
        for (int i = 0; i < array.length; i++) {
            s.append(",'")
                    .append(array[i][0])
                    .append("','")
                    .append(array[i][1])
                    .append("','")
                    .append(array[i][2])
                    .append("'");
        }
        s.append(")");

        return s.toString();
    }

    public static String sqlAddWithHisto(Integer key, String URL, Map<Integer, Map<Integer, Float>> resultMap, float black, float grey, float white, float lg) {

        StringBuilder s = new StringBuilder("INSERT IGNORE INTO imagesHSV (Img_id,URL");
//
//        for (Map.Entry p: resultMap.entrySet()) {
//            if ((int) p.getKey() == 0) {
//                Map<Integer, Float> m = (TreeMap<Integer, Float>) p.getValue();
//                for (Map.Entry k : m.entrySet()) {
//                    s.append(",R" + k.getValue());
//                }
//            }
//        }

        s.append(mapStringRequest("R", resultMap.get(0)));
        s.append(mapStringRequest("O", resultMap.get(1)));
        s.append(mapStringRequest("Y", resultMap.get(2)));
        s.append(mapStringRequest("LY", resultMap.get(3)));
        s.append(mapStringRequest("LG", resultMap.get(4)));
        s.append(mapStringRequest("G", resultMap.get(5)));
        s.append(mapStringRequest("LB", resultMap.get(6)));
        s.append(mapStringRequest("B", resultMap.get(7)));
        s.append(mapStringRequest("DB", resultMap.get(8)));
        s.append(mapStringRequest("P", resultMap.get(9)));
        s.append(mapStringRequest("DP", resultMap.get(10)));
        s.append(mapStringRequest("Pink", resultMap.get(11)));
        s.append(",Black");
        s.append(",Grey");
        s.append(",White");
        s.append(",LG");

        s.append(") VALUES ('")
                .append(key)
                .append("','")
                .append(URL)
                .append("'");
        s.append(mapColorStringRequest(resultMap.get(0)));
        s.append(mapColorStringRequest(resultMap.get(1)));
        s.append(mapColorStringRequest(resultMap.get(2)));
        s.append(mapColorStringRequest(resultMap.get(3)));
        s.append(mapColorStringRequest(resultMap.get(4)));
        s.append(mapColorStringRequest(resultMap.get(5)));
        s.append(mapColorStringRequest(resultMap.get(6)));
        s.append(mapColorStringRequest(resultMap.get(7)));
        s.append(mapColorStringRequest(resultMap.get(8)));
        s.append(mapColorStringRequest(resultMap.get(9)));
        s.append(mapColorStringRequest(resultMap.get(10)));
        s.append(mapColorStringRequest(resultMap.get(11)));
        s.append(",'" + black + "'");
        s.append(",'" + grey + "'");
        s.append(",'" + white + "'");
        s.append(",'" + lg + "'");
        s.append(")");


        return s.toString();
    }

    public static String mapStringRequest(String name, Map<Integer, Float> map) {
        String s = "";
        for (Map.Entry m : map.entrySet()) {
            s += "," + name + m.getKey();
        }
        return s;
    }

    public static String mapColorStringRequest(Map<Integer, Float> map) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry m : map.entrySet()) {
            s.append(",'").append(m.getValue()).append("'");

        }
        return s.toString();
    }


}
