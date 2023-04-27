/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service.impl;

import cl.afterlife.downloader.client.DownloadClient;
import cl.afterlife.downloader.client.Y2Client;
import cl.afterlife.downloader.client.YoutubeClient;
import cl.afterlife.downloader.client.builder.BuilderClient;
import cl.afterlife.downloader.config.Properties;
import cl.afterlife.downloader.dto.TraceDto;
import cl.afterlife.downloader.enumeration.DownloaderApplicationEnum;
import cl.afterlife.downloader.service.Y2DownloaderService;
import cl.afterlife.downloader.service.YoutubePlaylistDownloaderService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * YoutubePlaylistDownloaderServiceImpl
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Log4j2
@Service
public class YoutubePlaylistDownloaderServiceImpl implements YoutubePlaylistDownloaderService, Y2DownloaderService {

    @Autowired
    private Formatter formatter;

    @Autowired
    private Properties properties;

    @Autowired
    private Builder builder;

    @Autowired
    private BuilderClient builderClient;

    @Override
    public ResponseEntity<Map<String, Object>> downloadVideosOfPlayListToMp3(String playlistUrl, String apikey) {
        try {

            Y2Client y2Client = (Y2Client) this.builderClient.createClient(DownloaderApplicationEnum.Y2MATE_URL.getValueInString(), Y2Client.class);
            YoutubeClient youtubeClient = (YoutubeClient) this.builderClient.createClient(DownloaderApplicationEnum.YOUTUBE_URL.getValueInString(), YoutubeClient.class);

            ObjectMapper objectMapper = formatter.getObjectMapper();

            ResponseEntity<JsonNode> playlistItemsRs;
            JsonNode playlistItemsJsonRs = objectMapper.createObjectNode();

            List<String> playlistItems = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            /* Get identifiers of videos */
            if (!playlistUrl.contains(DownloaderApplicationEnum.LIST.getValueInString())) {
                return this.builder.createError("The link of playlist should have 'list='.", HttpStatus.PRECONDITION_FAILED);
            }

            String playlistId = playlistUrl.split(DownloaderApplicationEnum.LIST.getValueInString())[1];

            do {

                if (playlistItems.size() > 0) {
                    playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, playlistItemsJsonRs.get(DownloaderApplicationEnum.NEXT_PAGE_TOKEN.getValueInString()).asText());
                } else {
                    playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, "");
                }

                if (!playlistItemsRs.getStatusCode().is2xxSuccessful()) {
                    return this.builder.createError("The status code of youtube playlistItems microservice was: ".concat(playlistItemsRs.getStatusCode().toString()), HttpStatus.valueOf(playlistItemsRs.getStatusCode().value()));
                }

                playlistItemsJsonRs = playlistItemsRs.getBody();

                if (Objects.isNull(playlistItemsJsonRs) || !playlistItemsRs.hasBody()) {
                    return this.builder.createError("The response of youtube playlistItems microservice was empty or null", HttpStatus.NO_CONTENT);
                }

                ArrayNode playListItemsJsonArrayRs = ((ArrayNode) playlistItemsJsonRs.get("items"));

                if (playListItemsJsonArrayRs.size() == 0) {
                    return this.builder.createError("The response of youtube playlistItems microservice was: ".concat(formatter.writeValueAsJsonString(playlistItemsRs.getBody())), HttpStatus.NO_CONTENT);
                }

                List<String> withoutDuplicates = StreamSupport.stream(playListItemsJsonArrayRs.spliterator(), false)
                        .parallel()
                        .filter(item -> !playlistItems.contains(item
                                .get(DownloaderApplicationEnum.SNIPPET.getValueInString())
                                .get("title").asText().concat(" - videoId:").concat(item.get(DownloaderApplicationEnum.SNIPPET.getValueInString())
                                .get("resourceId").get("videoId").asText())))
                        .map(item -> item
                                .get(DownloaderApplicationEnum.SNIPPET.getValueInString())
                                .get("title").asText().concat(" - videoId:").concat(item.get(DownloaderApplicationEnum.SNIPPET.getValueInString())
                                .get("resourceId").get("videoId").asText()))
                        .collect(Collectors.toList());

                playlistItems.addAll(withoutDuplicates);

                if (withoutDuplicates.isEmpty()) {
                    break;
                }

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
                return this.builder.createError(String.format("The playlist was download in MP3 with errors. [ Total download: %1$s || Total found: %2$s]",
                                playlistItems.size() - errors.size(), playlistItems.size()),
                        errors,
                        HttpStatus.CONFLICT);
            }

            return this.builder.createResponse(String.format("The playlist was download complete in MP3. [ Total download: %1$s ]", playlistItems.size()));

        } catch (Exception ex) {
            return this.builder.createError(String.valueOf(ex), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void download(String dLink, String item, List<String> errors) {

        try {

            String videoName = item.split(DownloaderApplicationEnum.VIDEO_ID.getValueInString())[0].replaceAll("[^a-zA-Z0-9]", "").trim();

            StringBuilder downloadPathSb = new StringBuilder();
            downloadPathSb.append(this.properties.getYoutubeMp3FilesLocation());
            downloadPathSb.append("\\");
            downloadPathSb.append(videoName);
            downloadPathSb.append(".");
            downloadPathSb.append(DownloaderApplicationEnum.MP3.getValueInString());

            DownloadClient downloadClient = (DownloadClient) this.builderClient.createClient(dLink, DownloadClient.class);
            byte[] downloadResources = downloadClient.downloadDlink(URLEncoder.encode(dLink, StandardCharsets.UTF_8));
            FileUtils.writeByteArrayToFile(new File(downloadPathSb.toString()), downloadResources);
            log.info("Downloaded: {}", downloadPathSb);

        } catch (FeignException | IOException ex) {
            errors.add(item.concat(DownloaderApplicationEnum.DETAIL_ERROR.getValueInString()).concat(String.valueOf(ex)));
        }

    }

}
