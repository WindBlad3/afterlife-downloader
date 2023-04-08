/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service.impl;

import cl.afterlife.downloader.client.DownloadClient;
import cl.afterlife.downloader.client.Y2Client;
import cl.afterlife.downloader.client.YoutubeClient;
import cl.afterlife.downloader.client.builder.BuilderClient;
import cl.afterlife.downloader.dto.TraceDto;
import cl.afterlife.downloader.enumeration.DownloaderApplicationEnum;
import cl.afterlife.downloader.service.YoutubeDownloaderService;
import cl.afterlife.downloader.util.Builder;
import cl.afterlife.downloader.util.Formatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * YoutubeDownloaderServiceImpl
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Log4j2
@Service
public class YoutubeDownloaderServiceImpl implements YoutubeDownloaderService {

    @Autowired
    private Formatter formatter;

    @Autowired
    private Builder builder;

    @Autowired
    private BuilderClient builderClient;

    @Override
    public ResponseEntity<Map<String, Object>> downloadVideosOfPlayListToMp3(String playlistUrl, String apikey) {
        try {

            ObjectMapper objectMapper = formatter.getObjectMapper();

            ResponseEntity<JsonNode> playlistItemsRs;
            JsonNode playlistItemsJsonRs = objectMapper.createObjectNode();

            Y2Client y2Client = (Y2Client) builderClient.createClient(DownloaderApplicationEnum.Y2MATE_URL.getValueInString(), Y2Client.class);
            YoutubeClient youtubeClient = (YoutubeClient) builderClient.createClient(DownloaderApplicationEnum.YOUTUBE_URL.getValueInString(), YoutubeClient.class);

            List<String> playlistItems = new LinkedList<>();
            List<String> errors = new LinkedList<>();

            /* Get identifiers of videos */
            if (!playlistUrl.contains(DownloaderApplicationEnum.LIST.getValueInString())) {
                return builder.createError("The playlist should have '='.", HttpStatus.PRECONDITION_FAILED);
            }

            String playlistId = playlistUrl.split(DownloaderApplicationEnum.LIST.getValueInString())[1];

            do {

                if (playlistItems.size() > 0) {
                    playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, playlistItemsJsonRs.get(DownloaderApplicationEnum.NEXT_PAGE_TOKEN.getValueInString()).asText());
                } else {
                    playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, "");
                }

                if (!playlistItemsRs.getStatusCode().is2xxSuccessful()) {
                    return builder.createError("The status code of youtube playlistItems microservice was: ".concat(playlistItemsRs.getStatusCode().toString()), HttpStatus.valueOf(playlistItemsRs.getStatusCode().value()));
                }

                playlistItemsJsonRs = playlistItemsRs.getBody();

                if (Objects.isNull(playlistItemsJsonRs) || !playlistItemsRs.hasBody()) {
                    return builder.createError("The response of youtube playlistItems microservice was empty or null", HttpStatus.NO_CONTENT);
                }

                ArrayNode playListItemsJsonArrayRs = ((ArrayNode) playlistItemsJsonRs.get("items"));

                if (playListItemsJsonArrayRs.size() == 0) {
                    return builder.createError("The response of youtube playlistItems microservice was: ".concat(formatter.writeValueAsJsonString(playlistItemsRs.getBody())), HttpStatus.NO_CONTENT);
                }

                playlistItems.addAll(
                        StreamSupport.stream(playListItemsJsonArrayRs.spliterator(), false)
                                .parallel()
                                .map(item -> item.get(DownloaderApplicationEnum.SNIPPET.getValueInString()).get("title").asText().concat(" - videoId:").concat(item.get(DownloaderApplicationEnum.SNIPPET.getValueInString()).get("resourceId").get("videoId").asText()))
                                .collect(Collectors.toList())
                );

            } while (playlistItemsJsonRs.has(DownloaderApplicationEnum.NEXT_PAGE_TOKEN.getValueInString()));


            ForkJoinPool forkJoinPool = new ForkJoinPool(30);

            forkJoinPool.submit(() -> playlistItems.parallelStream().forEach(item -> {

                try {

                    String itemId = item.split(DownloaderApplicationEnum.VIDEO_ID.getValueInString())[1];

                    /* Get value K of video to download */
                    TraceDto traceGetK = this.getK("https://youtu.be/".concat(itemId), y2Client);

                    if (!traceGetK.getProcessedCorrectly()) {
                        errors.add(item.concat(DownloaderApplicationEnum.DETAIL_ERROR.getValueInString()).concat(traceGetK.getMessage()));
                    } else {
                        /* Get value DLink of video to download */
                        TraceDto traceGetDlink = this.getDlink(itemId, traceGetK.getValue(), y2Client);

                        if (!traceGetDlink.getProcessedCorrectly()) {
                            errors.add(item.concat(DownloaderApplicationEnum.DETAIL_ERROR.getValueInString()).concat(traceGetDlink.getMessage()));
                        }

                        /* Download */
                        this.download(traceGetDlink.getValue(), item, errors);
                    }

                } catch (Exception ex) {
                    errors.add(item.concat(DownloaderApplicationEnum.DETAIL_ERROR.getValueInString()).concat(String.valueOf(ex)));
                }

            })).get();

            if (!errors.isEmpty()) {
                return builder.createError(String.format("The playlist was download with errors. [ Total download: %1$s || Total found: %2$s]",
                                playlistItems.size() - errors.size(), playlistItemsJsonRs.get(DownloaderApplicationEnum.PAGE_INFO.getValueInString())
                                        .get(DownloaderApplicationEnum.TOTAL_RESULTS.getValueInString())),
                        errors,
                        HttpStatus.CONFLICT);
            }

            return builder.createResponse(String.format("The playlist was download complete. [ Total download: %1$s || Total found: %2$s]",
                    playlistItems.size(), playlistItemsJsonRs.get(DownloaderApplicationEnum.PAGE_INFO.getValueInString()).get(DownloaderApplicationEnum.TOTAL_RESULTS.getValueInString())));

        } catch (Exception ex) {
            return builder.createError(String.valueOf(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void download(String dLink, String item, List<String> errors) {

        try {

            String itemName = item.split(DownloaderApplicationEnum.VIDEO_ID.getValueInString())[0].replaceAll("[^a-zA-Z0-9]", "").trim();

            StringBuilder downloadPathSb = new StringBuilder();
            downloadPathSb.append(Paths.get("").toAbsolutePath());
            downloadPathSb.append("\\src\\main\\resources\\downloads\\");
            downloadPathSb.append(DownloaderApplicationEnum.MP3.getValueInString());
            downloadPathSb.append("\\");
            downloadPathSb.append(itemName);
            downloadPathSb.append(".");
            downloadPathSb.append(DownloaderApplicationEnum.MP3.getValueInString());

            DownloadClient downloadClient = (DownloadClient) builderClient.createClient(dLink, DownloadClient.class);
            byte[] downloadResources = downloadClient.downloadDlink(URLEncoder.encode(dLink, StandardCharsets.UTF_8));
            FileUtils.writeByteArrayToFile(new File(downloadPathSb.toString()), downloadResources);
            log.info("Downloaded: {}", downloadPathSb);

        } catch (FeignException | IOException ex) {
            errors.add(item.concat(DownloaderApplicationEnum.DETAIL_ERROR.getValueInString()).concat(String.valueOf(ex)));
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
