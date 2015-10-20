package ru.mirea.oop.practice.coursej.s131250;

import com.squareup.okhttp.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import retrofit.Call;
import ru.mirea.oop.practice.coursej.Configuration;
import ru.mirea.oop.practice.coursej.vk.Photos;
import ru.mirea.oop.practice.coursej.vk.Result;
import ru.mirea.oop.practice.coursej.vk.VkApi;
import ru.mirea.oop.practice.coursej.vk.entities.UploadServer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;

public class WAAction {
    private static final Logger logger = LoggerFactory.getLogger(WAAction.class);
    private ImageBuilder currentImage;
    private VkApi api;
    private String VkPhotoOptions;
    private WAMessage waMessage;

    public WAAction(VkApi api) {
        this.currentImage = new ImageBuilder();
        this.api = api;
    }

    public WAMessage getWAMessage(String input) {
        try {
            createImageFromWA(input);
            uploadPhotoToVk();
            generateWAMessage();
            return waMessage;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new WAMessage("WA parse error", null);
    }

    private void createImageFromWA(String input) throws IOException {
        ResponseBody waResponse = WARequestAction.getResponsefromWA(input);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            assert waResponse != null;
            Document doc = dBuilder.parse(waResponse.byteStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("pod");
            for (int ipod = 0; ipod < nList.getLength(); ipod++) {
                Node nNode = nList.item(ipod);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    currentImage.writeTextOnImage(eElement.getAttribute("title") + "\n");
                    NodeList subnods = eElement.getElementsByTagName("subpod");
                    for (int inod = 0; inod < subnods.getLength(); inod++) {
                        Node nSubNode = subnods.item(inod);
                        if (nSubNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element subElem = (Element) nSubNode;
                            Element imgElem = (Element) subElem.getElementsByTagName("img").item(0);
                            currentImage.pasteImageFromURL(imgElem.getAttribute("src"));
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void uploadPhotoToVk() throws IOException {
        Photos userver = api.getPhotos();
        Call<Result<UploadServer>> m2 = userver.getMessagesUploadServer();
        UploadServer mu = Result.call(m2);
        OkHttpClient client = new OkHttpClient();
        File file = new File(currentImage.getFullFileName());

        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("photo", file.getName(),
                        RequestBody.create(MediaType.parse("image/gif"), file))
                .build();

        Request request = new Request.Builder()
                .url(mu.upload_url)
                .post(requestBody)
                .build();
        Response resp2 = client.newCall(request).execute();

        if (!file.delete()) {logger.error("Ошибка удаления файла "+ Configuration.getFileName(file.getName()));}
        if (!resp2.isSuccessful()) {logger.error("Unexpected code "+ resp2);}
        String photo = resp2.body().string();
        JSONObject obj2 = new JSONObject(photo);
        Integer serverS = obj2.getInt("server");
        String photoS = obj2.getString("photo");
        String hash = obj2.getString("hash");
        Photos p = api.getPhotos();
        Call<Result<Object>> pcall = p.saveMessagesPhoto(serverS, photoS, photoS, hash);
        VkPhotoOptions = Result.call(pcall).toString();
    }

    private void generateWAMessage() throws IOException {
        String mediaId = VkPhotoOptions.split(", id=")[1].split(", aid")[0];
        String srcXxBig = "";
        String srcXxxBig = "";
        String messageText = "";
        try {
            srcXxBig = VkPhotoOptions.split(", src_xxbig=")[1].split(", ")[0];
            srcXxxBig = VkPhotoOptions.split(", src_xxxbig=")[1].split(", ")[0];
        } catch (Exception ignored) {
        }
        if (!srcXxBig.equals("")) {
            messageText = srcXxBig;
        }
        if (!srcXxxBig.equals("")) {
            messageText = srcXxxBig;
        }
        if (!messageText.equals("")) {
            messageText = "Original image: " + messageText;
        }
        waMessage = new WAMessage(messageText, mediaId);
    }


}
