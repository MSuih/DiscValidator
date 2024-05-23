package fi.smaragdi.discvalidator.ird.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FlexByIrdSource implements IrdSource {
    public static final String LIST_ALL_URL = "https://flexby420.github.io/playstation_3_ird_database/all.json";
    public static final String DOWNLOAD_URL = "https://github.com/FlexBy420/playstation_3_ird_database/raw/main/";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .followRedirects(true)
            .build();
    private final Request listAllRequest = new Request.Builder()
            .url(LIST_ALL_URL)
            .get()
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private record FlexByIrdInfo(
            @JsonProperty("title") String title,
            @JsonProperty("fw-ver") String firmwareVersion,
            @JsonProperty("game-ver") String gameVersion,
            @JsonProperty("app-ver") String appVersion,
            @JsonProperty("link") String link
    ) implements IrdInfo {
        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getFirmwareVersion() {
            return firmwareVersion;
        }

        @Override
        public String getGameVersion() {
            return gameVersion;
        }

        @Override
        public String getAppVersion() {
            return appVersion;
        }
    }

    @Override
    public Collection<? extends IrdInfo> listMatchingIrds(String serial) {
        try (Response response = client.newCall(listAllRequest).execute()) {
            if (response.body() == null) {
                throw new NullPointerException("Empty response from server");
            }
            Map<String, List<FlexByIrdInfo>> allIrds = mapper.readValue(response.body().byteStream(), new TypeReference<Map<String, List<FlexByIrdInfo>>>() {});
            return allIrds.getOrDefault(serial, Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream download(IrdInfo selection) {
        if (selection instanceof FlexByIrdInfo info) {
            Request request = new Request.Builder()
                    .url(DOWNLOAD_URL + info.link)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.body() == null) {
                    throw new NullPointerException("Empty response from server");
                }
                return response.body().byteStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Provided link is not from this ird source");
        }
    }
}
