package fi.smaragdi.discvalidator.ird.source;

import java.io.InputStream;
import java.util.Collection;

public interface IrdSource {
    Collection<? extends IrdInfo> listMatchingIrds(String serial);

    InputStream download(IrdInfo selection);

    interface IrdInfo {
        String getTitle();

        String getFirmwareVersion();

        String getGameVersion();

        String getAppVersion();
    }
}
