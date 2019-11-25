package io.qytc.stb;

public class StatusBean {
    private String cmd;
    private Data data;

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String acctno;
        private String camera;
        private String inRoom;
        private String mic;
        private String roomNo;
        private String speaker;
        private String deviceId;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getAcctno() {
            return acctno;
        }

        public void setAcctno(String acctno) {
            this.acctno = acctno;
        }

        public String getCamera() {
            return camera;
        }

        public void setCamera(String camera) {
            this.camera = camera;
        }

        public String getInRoom() {
            return inRoom;
        }

        public void setInRoom(String inRoom) {
            this.inRoom = inRoom;
        }

        public String getMic() {
            return mic;
        }

        public void setMic(String mic) {
            this.mic = mic;
        }

        public String getRoomNo() {
            return roomNo;
        }

        public void setRoomNo(String roomNo) {
            this.roomNo = roomNo;
        }

        public String getSpeaker() {
            return speaker;
        }

        public void setSpeaker(String speaker) {
            this.speaker = speaker;
        }
    }
}
