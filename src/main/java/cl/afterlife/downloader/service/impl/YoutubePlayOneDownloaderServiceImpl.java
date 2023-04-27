/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service.impl;

import cl.afterlife.downloader.client.DownloadClient;
import cl.afterlife.downloader.client.Y2Client;
import cl.afterlife.downloader.client.builder.BuilderClient;
import cl.afterlife.downloader.config.Properties;
import cl.afterlife.downloader.dto.TraceDto;
import cl.afterlife.downloader.enumeration.DownloaderApplicationEnum;
import cl.afterlife.downloader.service.Y2DownloaderService;
import cl.afterlife.downloader.service.YoutubePlayOneDownloaderService;
import cl.afterlife.downloader.util.Builder;
import cl.afterlife.downloader.util.Formatter;
import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YoutubePlayOneDownloaderServiceImpl
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-08
 */
@Log4j2
@Service
public class YoutubePlayOneDownloaderServiceImpl implements YoutubePlayOneDownloaderService, Y2DownloaderService {

    @Autowired
    private Formatter formatter;

    @Autowired
    private Properties properties;

    @Autowired
    private Builder builder;

    @Autowired
    private BuilderClient builderClient;

    @Override
    public ResponseEntity<Map<String, Object>> downloadVideoToMp3(String videoUrl, String videoName) {
        try {

            List<String> errors = new ArrayList<>();

            try {

                /* Get identifiers of videos */
                if (!videoUrl.contains(DownloaderApplicationEnum.V.getValueInString())) {
                    return this.builder.createError("The link of video should have 'v='.", HttpStatus.PRECONDITION_FAILED);
                }

                Y2Client y2Client = (Y2Client) this.builderClient.createClient(DownloaderApplicationEnum.Y2MATE_URL.getValueInString(), Y2Client.class);

                String itemId = videoUrl.split(DownloaderApplicationEnum.V.getValueInString())[1];

                /* Get value K of video to download */
                TraceDto traceGetK = this.getK("https://youtu.be/".concat(itemId), y2Client);

                if (!traceGetK.getProcessedCorrectly()) {
                    errors.add(traceGetK.getMessage());
                } else {
                    /* Get value DLink of video to download */
                    TraceDto traceGetDlink = this.getDlink(itemId, traceGetK.getValue(), y2Client);

                    if (!traceGetDlink.getProcessedCorrectly()) {
                        errors.add(traceGetDlink.getMessage());
                    }

                    /* Download */
                    this.download(traceGetDlink.getValue(), videoName, errors);
                }

            } catch (Exception ex) {
                errors.add(String.valueOf(ex));
            }

            if (!errors.isEmpty()) {
                return this.builder.createError(
                        "The video can not download in MP3!",
                        errors,
                        HttpStatus.CONFLICT);
            }

            return this.builder.createResponse("The video was download complete in MP3");

        } catch (Exception ex) {
            return this.builder.createError(String.valueOf(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void download(String dLink, String videoName, List<String> errors) {

        try {

            String dLinkCleaned = dLink.trim();

            StringBuilder downloadPathSb = new StringBuilder();
            downloadPathSb.append(this.properties.getYoutubeMp3FilesLocation());
            downloadPathSb.append("\\");
            downloadPathSb.append(videoName);
            downloadPathSb.append(".");
            downloadPathSb.append(DownloaderApplicationEnum.MP3.getValueInString());

            DownloadClient downloadClient = (DownloadClient) this.builderClient.createClient(dLinkCleaned, DownloadClient.class);
            byte[] downloadResource = downloadClient.downloadDlink(URLEncoder.encode(dLinkCleaned, StandardCharsets.UTF_8));
            FileUtils.writeByteArrayToFile(new File(downloadPathSb.toString()), downloadResource);

            log.info("Downloaded: {}", downloadPathSb);

        } catch (FeignException | IOException ex) {
            errors.add(String.valueOf(ex));
        }

    }

}
