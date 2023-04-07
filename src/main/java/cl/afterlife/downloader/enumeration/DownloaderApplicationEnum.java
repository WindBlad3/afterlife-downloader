/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.enumeration;


/**
 * DownloaderApplicationEnum
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
public enum DownloaderApplicationEnum {

    PAGE_INFO("pageInfo"),
    TOTAL_RESULTS("totalResults"),
    LINKS("links"),
    SNIPPET("snippet"),
    MP3128("mp3128"),
    NEXT_PAGE_TOKEN("nextPageToken"),
    LIST("list="),
    Y2MATE_URL("https://www.y2mate.com/mates"),
    YOUTUBE_URL("https://youtube.googleapis.com/youtube/v3"),
    DETAIL_ERROR(" - Detail error: "),
    VIDEO_ID("- videoId:"),
    D_LINK("dlink"),
    C_STATUS("c_status"),
    FAILED("FAILED"),
    K_PAGE("Youtube Downloader"),
    HI("en"),
    Q_AUTO(1),
    MP3("mp3");

    private final Object value;

    DownloaderApplicationEnum(Object value) {
        this.value = value;
    }

    public String getValueInString() {
        return String.valueOf(this.value);
    }

    public Integer getValueInInteger() {
        return Integer.valueOf(this.value.toString());
    }

}