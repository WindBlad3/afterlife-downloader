/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service;

import cl.afterlife.downloader.client.DownloadClient;
import cl.afterlife.downloader.client.Y2Client;
import cl.afterlife.downloader.dto.TraceDto;
import cl.afterlife.downloader.enumeration.DownloaderApplicationEnum;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Y2DownloaderService
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-27
 */
public interface DownloaderService {
    default TraceDto getDlink(String vid, String k, Y2Client y2Client) {

        TraceDto traceGetDlink = TraceDto.builder().build();

        try {

            Log4j2Holder.log.debug("Call the service convertV2Index with params: vid {}, k {}", vid, k);

            ResponseEntity<JsonNode> convertV2IndexRs = y2Client.convertV2Index(vid, k);

            Log4j2Holder.log.debug("End call the service convertV2Index with response: {}", convertV2IndexRs);

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

    default TraceDto getK(String videoUrl, Y2Client y2Client) {

        TraceDto traceGetK = TraceDto.builder().build();

        try {

            Log4j2Holder.log.debug("Call the service analyzeV2Ajax with params: k_query {}, k_page {}, h1 {}, q_auto {}", videoUrl, "Youtube Downloader", "en", 1);

            ResponseEntity<JsonNode> analyzeV2AjaxRs = y2Client.analyzeV2Ajax(videoUrl, DownloaderApplicationEnum.K_PAGE.getValueInString(),
                    DownloaderApplicationEnum.HI.getValueInString(), DownloaderApplicationEnum.Q_AUTO.getValueInInteger());

            Log4j2Holder.log.debug("End call the service analyzeV2Ajax with response: {}", analyzeV2AjaxRs);

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


    default void download(String filesLocation, String dLink, String videoName, List<String> errors, DownloadClient downloadClient) {
        try {

            StringBuilder downloadPathSb = new StringBuilder();
            downloadPathSb.append(filesLocation);
            downloadPathSb.append("\\");
            downloadPathSb.append(videoName.split(DownloaderApplicationEnum.VIDEO_ID.getValueInString())[0].replaceAll("[^a-zA-Z0-9]", "").trim());
            downloadPathSb.append(".");
            downloadPathSb.append(DownloaderApplicationEnum.MP3.getValueInString());

            byte[] downloadResource = downloadClient.downloadDlink(URLEncoder.encode(dLink, StandardCharsets.UTF_8));
            FileUtils.writeByteArrayToFile(new File(downloadPathSb.toString()), downloadResource);

            Log4j2Holder.log.info("Downloaded: {}", downloadPathSb);

        } catch (FeignException | IOException ex) {
            errors.add(String.valueOf(ex));
        }

    }

    @Log4j2
    final class Log4j2Holder {
    }
}
