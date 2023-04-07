package cl.afterlife.downloader.service.impl;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import cl.afterlife.downloader.client.Y2Client;
import cl.afterlife.downloader.client.YoutubeClient;
import cl.afterlife.downloader.dto.TraceDto;
import cl.afterlife.downloader.service.YoutubeDownloaderService;
import cl.afterlife.downloader.util.Builder;
import cl.afterlife.downloader.util.Formatter;
import feign.FeignException;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class YoutubeDownloaderServiceImpl implements YoutubeDownloaderService {

    @Autowired
    private Y2Client y2Client;

    @Autowired
    private YoutubeClient youtubeClient;

    @Autowired
    private Formatter formatter;

    @Autowired
    private Builder builder;
    
    @Override
    public ResponseEntity<Map<String,Object>> downloadVideosOfPlayListToMp3(String playlistUrl, String apikey) {
            try {
                
                ObjectMapper objectMapper = formatter.getObjectMapper();

                /* Get identifiers of videos */
                if (!playlistUrl.contains("list=")){
                    return builder.createError("The playlist should have '='.", HttpStatus.PRECONDITION_FAILED);
                }

                String playlistId = playlistUrl.split("list=")[1];

                List<String> playlistItems = new LinkedList<>();
                ResponseEntity<JsonNode> playlistItemsRs = ResponseEntity.ok().build();  
                JsonNode playlistItemsJsonRs = objectMapper.createObjectNode();

                do {
                    
                    if(playlistItems.size() > 0){
                        playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, playlistItemsJsonRs.get("nextPageToken").asText());
                    }else{
                        playlistItemsRs = youtubeClient.playlistItems(apikey, playlistId, "");
                    }

                    if (!playlistItemsRs.getStatusCode().is2xxSuccessful()){
                        return builder.createError("The status code of youtube playlistItems microservice was: ".concat(playlistItemsRs.getStatusCode().toString()),HttpStatus.valueOf(playlistItemsRs.getStatusCode().value()));
                    }
    
                    playlistItemsJsonRs = playlistItemsRs.getBody();
   
                    if(Objects.isNull(playlistItemsJsonRs) || !playlistItemsRs.hasBody()){
                        return builder.createError("The response of youtube playlistItems microservice was empty or null", HttpStatus.NO_CONTENT);
                    }

                    ArrayNode playListItemsJsonArrayRs = ((ArrayNode) playlistItemsJsonRs.get("items"));
    
                    if(playListItemsJsonArrayRs.size() == 0){
                        return builder.createError("The response of youtube playlistItems microservice was: ".concat(formatter.writeValueAsJsonString(playlistItemsRs.getBody())), HttpStatus.NO_CONTENT);
                    }

                    playlistItems.addAll(StreamSupport.stream(playListItemsJsonArrayRs.spliterator(), false)
                    .map( item -> item.get("snippet").get("title").asText().concat(" - videoId:").concat(item.get("snippet").get("resourceId").get("videoId").asText()))
                    .collect(Collectors.toList()));

                } while (playlistItemsJsonRs.has("nextPageToken"));


                List<String> errors = new LinkedList<>();

                for (String item : playlistItems) {

                    String itemId = item.split("- videoId:")[1];

                    /* Get value K of video to download */
                    TraceDto traceGetK = this.getK("https://youtu.be/".concat(itemId), objectMapper);

                    if(!traceGetK.getProcessedCorrectly()){
                        errors.add(item.concat(" - Detail error: ").concat(traceGetK.getMessage()));
                    }else{
                        /* Get value DLink of video to download */
                        TraceDto traceGetDlink = this.getDlink(itemId, traceGetK.getValue(), objectMapper);

                        if(!traceGetDlink.getProcessedCorrectly()){
                            errors.add(item.concat(" - Detail error: ").concat(traceGetDlink.getMessage()));
                        }

                        try {
                            URL myURL = new URL(traceGetDlink.getValue());
                            URLConnection myURLConnection = myURL.openConnection();
                            myURLConnection.connect();
                        } 
                        catch (Exception e) {
                            errors.add(item.concat(" - Detail error: ").concat(e.getMessage()));
                        } 
    
                    }
                }

                if(!errors.isEmpty()){

                    return builder.createError(String.format("The playlist was download with errors. [ Total download: %1$s || Total found: %2$s]",
                    playlistItems.size() - errors.size(), playlistItemsJsonRs.get("pageInfo").get("totalResults")),
                    errors,
                    HttpStatus.CONFLICT);
                }
                
                return builder.createResponse(String.format("The playlist was download complete. [ Total download: %1$s || Total found: %2$s]",
                    playlistItems.size(), playlistItemsJsonRs.get("pageInfo").get("totalResults")));

            } catch (Exception ex) {
                return builder.createError(String.valueOf(ex), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        
    }

    private TraceDto getK(String videoUrl, ObjectMapper objectMapper) {

        TraceDto traceGetK = TraceDto.builder().build();

        try {

            log.debug("Call the service analyzeV2Ajax with params: k_query {}, k_page {}, h1 {}, q_auto {}",videoUrl,"Youtube Downloader", "en", 1);

            ResponseEntity<JsonNode> analyzeV2AjaxRs = y2Client.analyzeV2Ajax(videoUrl, "Youtube Downloader", "en", 1);

            log.debug("End call the service analyzeV2Ajax with response: {}", analyzeV2AjaxRs);   

            if(!analyzeV2AjaxRs.getStatusCode().is2xxSuccessful()){
                traceGetK.setMessage("The status code of analyzeV2Ajax microservice was: ".concat(analyzeV2AjaxRs.getStatusCode().toString()));
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }
    
            JsonNode analyzeV2AjaxJsonRs = analyzeV2AjaxRs.getBody();
    
            if(Objects.isNull(analyzeV2AjaxJsonRs) || !analyzeV2AjaxRs.hasBody()){
                traceGetK.setMessage("The response of analyzeV2Ajax microservice was empty or null");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }
       
            if(analyzeV2AjaxJsonRs.has("c_status") && analyzeV2AjaxJsonRs.get("c_status").asText().equalsIgnoreCase("FAILED")){
                traceGetK.setMessage("The analyzeV2Ajax microservice can not convert the video!");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }
    
            if(!analyzeV2AjaxJsonRs.get("links").get("mp3").has("mp3128")){
                traceGetK.setMessage("The mp3128 in analyzeV2Ajax response not exists");
                traceGetK.setProcessedCorrectly(false);
                return traceGetK;
            }
    
            traceGetK.setValue(analyzeV2AjaxJsonRs.get("links").get("mp3").get("mp3128").get("k").asText());
            traceGetK.setProcessedCorrectly(true);

        } catch (FeignException ex) {
            traceGetK.setMessage(String.valueOf(ex));
            traceGetK.setProcessedCorrectly(false);
            return traceGetK;
        }

        return traceGetK;

    }

    private TraceDto getDlink(String vid, String k, ObjectMapper objectMapper) {
   
        TraceDto traceGetDlink = TraceDto.builder().build();

        try {

        log.debug("Call the service convertV2Index with params: vid {}, k {}", vid, k);
        
        ResponseEntity<JsonNode> convertV2IndexRs = y2Client.convertV2Index(vid, k);

        log.debug("End call the service convertV2Index with response: {}", convertV2IndexRs);

        if(!convertV2IndexRs.getStatusCode().is2xxSuccessful()){
            traceGetDlink.setMessage("The status code of convertV2Index microservice was: ".concat(convertV2IndexRs.getStatusCode().toString()));
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        }

        JsonNode convertV2IndexJsonRs = convertV2IndexRs.getBody();

        if(Objects.isNull(convertV2IndexJsonRs) || !convertV2IndexRs.hasBody()){
            traceGetDlink.setMessage("The response of convertV2Index microservice was empty or null");
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        }

        if(convertV2IndexJsonRs.has("c_status") && convertV2IndexJsonRs.get("c_status").asText().equalsIgnoreCase("FAILED")){
            traceGetDlink.setMessage("The convertV2Index microservice can not convert the video!");
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        }

        if(!convertV2IndexJsonRs.has("dlink")){
            traceGetDlink.setMessage("The dlink in convertV2Index response not exists");
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        } 

        traceGetDlink.setValue(convertV2IndexJsonRs.get("dlink").asText());
        traceGetDlink.setProcessedCorrectly(true);

        } catch (FeignException ex) {
            traceGetDlink.setMessage(String.valueOf(ex));
            traceGetDlink.setProcessedCorrectly(false);
            return traceGetDlink;
        }

        return traceGetDlink;

    }
    
}
