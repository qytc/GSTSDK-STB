package io.qytc.stb;

public interface Urls {

    /**
     * UMS 服务器主路径IP
     */
    String MAIN_IP = "ycdj.whqunyu.com";

    /**
     * UMS 服务器端口
     */
    String PORT = "9999";

    /**
     * UMS API地址
     */
    String BASE_URL = "http://" + MAIN_IP + ":" + PORT + "/api/";

    /**
     * websocket地址
     */
    String KEEPLIVE_URL = "ws://" + MAIN_IP + ":" + PORT + "/ws/msg";

    /**
     * 加入房间
     */
    String JOIN_ROOM = "meeting/joinRoom";

    /**
     * 退出房间
     */
    String EXIT_ROOM = "meeting/exitRoom";

    /**
     * 申请发言
     */
    String REQUEST_SPEAK = "live/requestSpeak";

    /**
     * 获取签名
     */
    String GENERATE_USER_SIG = "v1/generateUserSig";

    /**
     * 取消发言
     */
    String CANCEL_SPEAK = "live/cancelSpeak";
}
