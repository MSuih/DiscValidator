package fi.smaragdi.discvalidator.ird.source;

import java.io.InputStream;
import java.util.List;

public interface IrdSource<T extends IrdSource.IrdLink> {
    List<T> listMatchingIrds(String serial);
    InputStream download(T selection);

    interface IrdLink {
        String getTitle();
        String getFirmwareVersion();
        String getGameVersion();
        String getAppVersion();
    }
}
