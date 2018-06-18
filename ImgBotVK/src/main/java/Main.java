
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
    private static int offset = 1;
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


        //получение списка URL с БД
        ResultSet resultSet = statement.executeQuery("select URL from images");
        while (resultSet.next()) {
            idList.add(resultSet.getString(1));
        }


        while (true) {
            List<Photo> list = getPhotoFromVK(offset, apiClient, actor);
            HashMap<String, String> map = downloadImg(list);
            for (Map.Entry<String, String> m : map.entrySet()) {
                System.out.println(m.getKey() + " " + m.getValue());
            }

            try {

                for (Map.Entry<String, String> m : map.entrySet()) {
                    statement.addBatch("INSERT IGNORE INTO images (URL,color) VALUES ('" + m.getKey() + "','" + m.getValue() + "')");
                }
                statement.executeBatch();
                statement.clearBatch();
            } catch (SQLException e) {
                System.out.println("ERROR" + e.getMessage());
            }
            offset+=100;

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

    public static void auth(String appId) throws IOException {
        String reqUrl = AUTH_URL
                .replace("{APP_ID}", appId)
                .replace("{PERMISSIONS}", "photos,messages")
                .replace("{REDIRECT_URI}", "https://oauth.vk.com/blank.html")
                .replace("{DISPLAY}", "page");
        try {
            Desktop.getDesktop().browse(new URL(reqUrl).toURI());
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
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

    public static HashMap<String, String> downloadImg(List<Photo> list) {
        File file = null;
        BufferedImage img=null;
        boolean isLoaded=true;
        HashMap<String, String> hashMap = new HashMap<String, String>();
        try {

            for (int i = 0; i < list.size(); i++) {
                String fileName = "google" + i + ".png";

                if (list.get(i).getPhoto807() != null) {
                    System.out.println("Мы попали в первую проверку");
                    if(list.get(i).getPhoto1280() != null){
                        System.out.println("Мы попали в проверку 1280");
                        img = ImageIO.read(new URL(list.get(i).getPhoto1280()));
//
                    }else {
                        System.out.println("Мы попали в проверку 807");
                        img = ImageIO.read(new URL(list.get(i).getPhoto807()));
//
                    }
                    if (isLoaded) {
                        System.out.println("загружаем");
                        file = new File(fileName);
                        if (!file.exists()) {
                            file.createNewFile();

                        }
                        BufferedImage scaled = new BufferedImage(1, 1,
                                BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = scaled.createGraphics();
                        g.drawImage(img, 0, 0, 1, 1, null);
                        g.dispose();


                        ImageIO.write(scaled, "png", file);

                        int blue = scaled.getRGB(0, 0) & 255;
                        int green = (scaled.getRGB(0, 0) >> 8) & 255;
                        int red = (scaled.getRGB(0, 0) >> 16) & 255;
                        String hex = String.format("#%02x%02x%02x", red, green, blue);
                        System.out.println(hex);
                        hashMap.put(list.get(i).getPhoto604(), hex);
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return hashMap;
    }

}
