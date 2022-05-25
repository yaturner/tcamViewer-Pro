package com.darcangel.acam.constants;

public final class Constants {
    public final static String SUCCESS = "{\"result\":\"OK\"}";
    public final static String ERROR = "{\"result\":\"ERROR\"}";

    public final static int IMAGE_HEIGHT = 120;
    public final static int IMAGE_WIDTH  = 160;

    public final static String KEY_CAMERA_IP_ADDRESS = "cam_address";

    public final static String CHAT_SERVER_URL = "http://192.168.0.42"; //TESTING ONLY

    public final static String CMD_GET_STATUS  = "\2{\"cmd\":\"get_status\"}\3";
    public final static String CMD_GET_CONFIG  = "\2{\"cmd\":\"get_config\"}\3";
    public final static String CMD_GET_WIFI    = "\2{\"cmd\":\"get_wifi\"}\3";
    public final static String CMD_SET_TIME    = "\2{\"cmd\":\"set_time\", \"args\": %s}\3";
    public final static String CMD_SET_CONFIG  = "\2{\"cmd\":\"set_config\", \"args\": %s}\3";
    public final static String CMD_SET_SPOTMETER   = "\2{\"cmd\":\"set_spotmeter\", \"args\": %s}\3";
    public final static String CMD_SET_STREAM_ON   = "\2{\"cmd\":\"set_stream_on\", \"args\": %s}\3";
    public final static String CMD_SET_STREAM_OFF   = "\2{\"cmd\":\"set_stream_off\", \"args\": %s}\3";
    public final static String CMD_SET_WIFI   = "\2{\"cmd\":\"set_wifi\"}\3";
    public final static String CMD_GET_IMAGE   = "\2{\"cmd\":\"get_image\"}\3";

    public final static String ARGS_SET_TIME   = "{\n" +
            "    \"sec\": %d,\n" +
            "    \"min\": %d,\n" +
            "    \"hour\": %d,\n" +
            "    \"dow\": %d,\n" +
            "    \"day\": %d,\n" +
            "    \"mon\": %d,\n" +
            "    \"year\": %d\n" +
            "  }";
    public final static String ARGS_SET_CONFIG = "{\n" +
            "    \"agc_enabled\": %d,\n" +
            "    \"emissivity\": %d,\n" +
            "    \"gain_mode\": %d\n" +
            "  }";
    public final static String ARGS_SET_SPOTMETER = "{\n" +
            "    \"c1\": %d,\n" +
            "    \"c2\": %d,\n" +
            "    \"r1\": %d\n" +
            "    \"r2\": %d\n" +
            "  }";
    public final static String ARGS_SET_STREAM_ON = "{\n" +
            "    \"delay_msec\":0,\n" +
            "    \"num_frames\":0\n" +
            "   }";
    public final static String ARGS_SET_WIFI_STATIC = "{\n" +
            "    \"ap_ssid\": \"%s\"\n" +
            "    \"ap_pw: \"%s\"\n" +
            "    \"ap_ip_addr\": \"%s\",\n" +
            "    \"flags\": %d,\n" +
            "    \"sta_ssid\": \"%s\",\n" +
            "    \"sta_pw\": \"%s\",\n" +
            "    \"sta_ip_addr\": \"%s\",\n" +
            "    \"sta_netmask\": \"%s\"\n" +
            "  }";
    public final static String ARGS_SET_WIFI = "{\n" +
            "    \"ap_ssid\": \"%s\"\n" +
            "    \"ap_pw: \"%s\"\n" +
            "    \"ap_ip_addr\": \"%s\",\n" +
            "    \"flags\": %d,\n" +
            "    \"sta_ssid\": \"%s\",\n" +
            "    \"sta_pw\": \"%s\",\n" +
            "  }";

    private Constants() {
    }
}