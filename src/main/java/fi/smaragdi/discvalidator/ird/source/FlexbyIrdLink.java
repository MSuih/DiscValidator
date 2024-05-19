package fi.smaragdi.discvalidator.ird.source;

import org.codehaus.jackson.annotate.JsonProperty;

public class FlexbyIrdLink implements IrdSource.IrdLink {
    private String title;
    @JsonProperty("fw-ver")
    private String firmwareVersion;
    @JsonProperty("game-ver")
    private String gameVersion;
    @JsonProperty("app-ver")
    private String appVersion;
    private String link;

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    @Override
    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
