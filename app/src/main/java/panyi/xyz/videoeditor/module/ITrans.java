package panyi.xyz.videoeditor.module;

import java.net.UnknownHostException;

public interface ITrans {
    /**
     * 开启服务
     * @param port
     */
    void startServer(int port , OnReceiveCallback callback);


    /**
     *  发送数据
     * @param data
     * @return 发送成功的字节数
     *
     */
    int sendData(String remoteAddress , int remotePort , byte[] data) throws UnknownHostException;

    /**
     * 关闭
     */
    void close();

    interface OnReceiveCallback{
        /**
         *  接收到数据
         * @param data
         */
        void onReceiveData(byte[] data);
    }
}
