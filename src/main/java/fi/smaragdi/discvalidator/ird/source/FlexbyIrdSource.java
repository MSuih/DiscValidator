package fi.smaragdi.discvalidator.ird.source;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FlexbyIrdSource implements IrdSource<FlexbyIrdLink> {
    public static final String LIST_ALL_URL = "https://flexby420.github.io/playstation_3_ird_database/all.json";
    public static final String DOWNLOAD_URL = "https://github.com/FlexBy420/playstation_3_ird_database/raw/main/";

    private final HttpClient client = HttpClient.newHttpClient();
    private final HttpRequest listAllRequest = HttpRequest.newBuilder()
            .uri(URI.create(LIST_ALL_URL))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

    @Override
    public List<FlexbyIrdLink> listMatchingIrds(String serial) {
        try {
            Map<String, List<FlexbyIrdLink>> response = client.send(listAllRequest, responseInfo -> irdLinkBodyHandler()).body().get();
            return response.getOrDefault(serial, Collections.emptyList());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream download(FlexbyIrdLink selection) {
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + selection.getLink()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            return client.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream()).body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse.BodySubscriber<Supplier<Map<String, List<FlexbyIrdLink>>>> irdLinkBodyHandler() {
        HttpResponse.BodySubscriber<InputStream> inputStreamBodySubscriber = HttpResponse.BodySubscribers.ofInputStream();
        return HttpResponse.BodySubscribers.mapping(inputStreamBodySubscriber, FlexbyIrdSource::toSupplierOfType);
    }

    public static Supplier<Map<String, List<FlexbyIrdLink>>> toSupplierOfType(InputStream inputStream) {
        return () -> {
            try (InputStream stream = inputStream) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(stream, new TypeReference<Map<String, List<IrdLink>>>(){});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
