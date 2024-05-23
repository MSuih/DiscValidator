package fi.smaragdi.discvalidator.ird.source;

import java.util.Collection;

public interface IrdSource {
    Collection<? extends IrdInfo> listMatchingIrds(String serial);
    byte[] download(IrdInfo selection);

    interface IrdInfo {
        String getTitle();
        String getFirmwareVersion();
        String getGameVersion();
        String getAppVersion();
    }
}
