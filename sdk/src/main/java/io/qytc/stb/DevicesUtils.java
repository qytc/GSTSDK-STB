package io.qytc.stb;

import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;

public class DevicesUtils {
    public static TRTCCloud checkOritation(TRTCCloud trtcCloud) {
        String carrier = android.os.Build.MANUFACTURER;//生产商,如 SkyworthDigital
        String deviceMode = android.os.Build.MODEL;//设备型号,如 Hi3796MV100
        if (("SkyworthDigital".equals(carrier) && "HC2900".equals(deviceMode)) ||
                ("Unionman".equals(carrier) && "Hi3796MV100".equals(deviceMode)) ||
                ("Hisilicon".equals(carrier) && "Hi3796MV100".equals(deviceMode)) ||
                ("MStar Semiconductor, Inc.".equals(carrier) && "MStar Android TV".equals(deviceMode)) ||
                ("Skyworth".equals(carrier) && "1S11_N9201".equals(deviceMode)) ||
                ("Skyworth".equals(carrier) && "GHD33_HC2910".equals(deviceMode))) {
            //创维3796,九联3796,股份3796,晨星，江苏创维N9201，江苏创维HC2910
            trtcCloud.setLocalViewMirror(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_DISABLE);
            trtcCloud.setLocalViewRotation(TRTCCloudDef.TRTC_VIDEO_ROTATION_180);
            trtcCloud.setVideoEncoderRotation(TRTCCloudDef.TRTC_VIDEO_ROTATION_180);
            trtcCloud.setVideoEncoderMirror(false);//画面不镜像
        } else if ("Unionman".equals(carrier) && "Hi3798MV200".equals(deviceMode) ||
                ("Yinhe".equals(carrier) && "Hi3796MV100".equals(deviceMode)) ||
                ("Hisense".equals(carrier) && "Hi3796MV100".equals(deviceMode)) ||
                ("ChangHong".equals(carrier) && "OTS-3000 HB".equals(deviceMode)) ||
                ("Hisilicon".equals(carrier) && "Hi3798MV200".equals(deviceMode)) ||
                ("SkyworthDigital".equals(carrier) && "HC2910".equals(deviceMode)) ||
                ("Geeya".equals(carrier) && "41".equals(deviceMode))) {
            //九联3798,银河3796,海信（聚好看）3796,长虹3000，股份3798，金亚41
            trtcCloud.setLocalViewRotation(TRTCCloudDef.TRTC_VIDEO_ROTATION_0);
            trtcCloud.setVideoEncoderRotation(TRTCCloudDef.TRTC_VIDEO_ROTATION_0);
            trtcCloud.setVideoEncoderMirror(true);//画面镜像
        }
        return trtcCloud;
    }

    public static boolean professionalDevice() {
        String carrier = android.os.Build.MANUFACTURER;//生产商
        String deviceMode = android.os.Build.MODEL;//设备型号
        if ("alps".equals(carrier) && "c4z".equals(deviceMode)) {
            return true;
        } else if ("alps".equals(carrier) && "c4pro".equals(deviceMode)) {
            return true;
        } else if ("alps".equals(carrier) && "nb6797_6c_m".equals(deviceMode)) {
            return true;
        }
        return false;
    }
}
