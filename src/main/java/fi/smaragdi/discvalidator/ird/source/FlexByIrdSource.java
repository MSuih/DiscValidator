package fi.smaragdi.discvalidator.ird.source;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FlexByIrdSource implements IrdSource {
    public static final String LIST_ALL_URL = "https://flexby420.github.io/playstation_3_ird_database/all.json";
    public static final String DOWNLOAD_URL = "https://github.com/FlexBy420/playstation_3_ird_database/raw/main/";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final HttpRequest listAllRequest = HttpRequest.newBuilder()
            .uri(URI.create(LIST_ALL_URL))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

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
        try {
            Map<String, List<? extends IrdInfo>> response = client.send(listAllRequest, _ -> irdLinkBodyHandler()).body().get();
            return response.getOrDefault(serial, Collections.emptyList());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream download(IrdInfo selection) {
        if (selection instanceof FlexByIrdInfo l) {
            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL + l.link()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            try {
                return client.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream()).body();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Provided link is not from this ird source");
        }

    }

    public HttpResponse.BodySubscriber<Supplier<Map<String, List<? extends IrdInfo>>>> irdLinkBodyHandler() {
        HttpResponse.BodySubscriber<InputStream> inputStreamBodySubscriber = HttpResponse.BodySubscribers.ofInputStream();
        return HttpResponse.BodySubscribers.mapping(inputStreamBodySubscriber, FlexByIrdSource::toResultSupplier);
    }

    public static Supplier<Map<String, List<? extends IrdInfo>>> toResultSupplier(InputStream inputStream) {
        return () -> {
            try (InputStream stream = inputStream) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(stream, new TypeReference<Map<String, List<FlexByIrdInfo>>>(){});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
