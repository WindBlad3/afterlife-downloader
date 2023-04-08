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
import cl.afterlife.downloader.service.YoutubePlayOneDownloaderService;
import cl.afterlife.downloader.util.Builder;
import cl.afterlife.downloader.util.Formatter;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Objects;

/**
 * YoutubePlayOneDownloaderServiceImpl
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-08
 */
@Log4j2
@Service
public class YoutubePlayOneDownloaderServiceImpl implements YoutubePlayOneDownloaderService {

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

            String downloadPathSb = this.properties.getYoutubeMp3FilesLocation() +
                    "\\" +
                    videoName +
                    "." +
                    DownloaderApplicationEnum.MP3.getValueInString();

            DownloadClient downloadClient = (DownloadClient) this.builderClient.createClient(dLinkCleaned, DownloadClient.class);
            byte[] downloadResource = downloadClient.downloadDlink(URLEncoder.encode(dLinkCleaned, StandardCharsets.UTF_8));
            FileUtils.writeByteArrayToFile(new File(downloadPathSb), downloadResource);

            log.info("Downloaded: {}", downloadPathSb);

        } catch (FeignException | IOException ex) {
            errors.add(String.valueOf(ex));
        }

    }

    private TraceDto getK(String videoUrl, Y2Client y2Client) {

        TraceDto traceGetK = TraceDto.builder().build();

        try {

            log.debug("Call the service analyzeV2Ajax with params: k_query {}, k_page {}, h1 {}, q_auto {}", videoUrl, "Youtube Downloader", "en", 1);

            ResponseEntity<JsonNode> analyzeV2AjaxRs = y2Client.analyzeV2Ajax(videoUrl, DownloaderApplicationEnum.K_PAGE.getValueInString(),
                    DownloaderApplicationEnum.HI.getValueInString(), DownloaderApplicationEnum.Q_AUTO.getValueInInteger());

            log.debug("End call the service analyzeV2Ajax with response: {}", analyzeV2AjaxRs);

            if (!analyzeV2AjaxRs.getStatusCode().is2xxSuccessful()) {
                traceGetK.setMessage("The status code of analyzeV2Ajax microservice was: ".concat(analyzeV2AjaxRs.getStatusCode().toString()));
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }

            JsonNode analyzeV2AjaxJsonRs = analyzeV2AjaxRs.getBody();

            if (Objects.isNull(analyzeV2AjaxJsonRs) || !analyzeV2AjaxRs.hasBody()) {
                traceGetK.setMessage("The response of analyzeV2Ajax microservice was empty or null");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }

            if (analyzeV2AjaxJsonRs.has(DownloaderApplicationEnum.C_STATUS.getValueInString())
                    && analyzeV2AjaxJsonRs.get(DownloaderApplicationEnum.C_STATUS.getValueInString()).asText().equalsIgnoreCase(DownloaderApplicationEnum.FAILED.getValueInString())) {
                traceGetK.setMessage("The analyzeV2Ajax microservice can not convert the video!");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }

            if (!analyzeV2AjaxJsonRs.get(DownloaderApplicationEnum.LINKS.getValueInString()).get(DownloaderApplicationEnum.MP3.getValueInString()).has(DownloaderApplicationEnum.MP3128.getValueInString())) {
                traceGetK.setMessage("The mp3128 in analyzeV2Ajax response not exists");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }

            traceGetK.setValue(analyzeV2AjaxJsonRs.get(DownloaderApplicationEnum.LINKS.getValueInString()).get(DownloaderApplicationEnum.MP3.getValueInString())
                    .get(DownloaderApplicationEnum.MP3128.getValueInString()).get("k").asText());
            traceGetK.setProcessedCorrectly(true);

        } catch (FeignException ex) {
            traceGetK.setMessage(String.valueOf(ex));
            traceGetK.setProcessedCorrectly(false);
            return traceGetK;
        }

        return traceGetK;

    }

    private TraceDto getDlink(String vid, String k, Y2Client y2Client) {

        TraceDto traceGetDlink = TraceDto.builder().build();

        try {

            log.debug("Call the service convertV2Index with params: vid {}, k {}", vid, k);

            ResponseEntity<JsonNode> convertV2IndexRs = y2Client.convertV2Index(vid, k);

            log.debug("End call the service convertV2Index with response: {}", convertV2IndexRs);

            if (!convertV2IndexRs.getStatusCode().is2xxSuccessful()) {
                traceGetDlink.setMessage("The status code of convertV2Index microservice was: ".concat(convertV2IndexRs.getStatusCode().toString()));
                traceGetDlink.setProcessedCorrectly(false);
                return traceGetDlink;
            }

            JsonNode convertV2IndexJsonRs = convertV2IndexRs.getBody();

            if (Objects.isNull(convertV2IndexJsonRs) || !convertV2IndexRs.hasBody()) {
                traceGetDlink.setMessage("The response of convertV2Index microservice was empty or null");
                traceGetDlink.setProcessedCorrectly(false);
                return traceGetDlink;
            }

            if (convertV2IndexJsonRs.has(DownloaderApplicationEnum.C_STATUS.getValueInString()) &&
                    convertV2IndexJsonRs.get(DownloaderApplicationEnum.C_STATUS.getValueInString()).asText().equalsIgnoreCase(DownloaderApplicationEnum.FAILED.getValueInString())) {
                traceGetDlink.setMessage("The convertV2Index microservice can not convert the video!");
                traceGetDlink.setProcessedCorrectly(false);
                return traceGetDlink;
            }

            if (!convertV2IndexJsonRs.has(DownloaderApplicationEnum.D_LINK.getValueInString())) {
                traceGetDlink.setMessage("The dlink in convertV2Index response not exists");
                traceGetDlink.setProcessedCorrectly(false);
                return traceGetDlink;
            }

            traceGetDlink.setValue(convertV2IndexJsonRs.get(DownloaderApplicationEnum.D_LINK.getValueInString()).asText());
            traceGetDlink.setProcessedCorrectly(true);

        } catch (FeignException ex) {
            traceGetDlink.setMessage(String.valueOf(ex));
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        }

        return traceGetDlink;

    }

}
