/**
 * DownloaderApplicationEnum
 */
public enum DownloaderApplicationEnum {

   K_PAGE("Youtube Downloader"),
   HI("en"),
   Q_AUTO("1");
   
   private String value;

   DownloaderApplicationEnum (String value){
    this.value = value;
   }

   public String getValue() {
       return this.value;
   }

}