package panyi.xyz.videoeditor.module;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import panyi.xyz.videoeditor.util.LogUtil;

/**
 * 基于UDP实现的传输
 */
public class UdpTrans extends  Thread implements ITrans{
    private final int BUF_SIZE = 1024 * 1024; //1M

    private int port = -1;

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private DatagramSocket socket;

    private OnReceiveCallback callback;

    private ExecutorService sendTaskThreadPool;

    @Override
    public void startServer(int port, OnReceiveCallback callback) {
        this.port = port;
        this.callback = callback;

        //启动网络线程
        start();
    }

    @Override
    public int sendData(String remoteAddress, int remotePort, byte[] data) throws UnknownHostException {
        if(socket == null || !isRunning.get() || sendTaskThreadPool == null){
            return -1;
        }

        sendTaskThreadPool.submit(()->{
            try {
                DatagramPacket sendPacket = new DatagramPacket(data , 0 , data.length);
                sendPacket.setPort(remotePort);
                sendPacket.setAddress(InetAddress.getByName(remoteAddress));
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return 0;
    }


    @Override
    public void close() {
        isRunning.set(false);

        if(socket != null){
            socket.close();
        }

        if(sendTaskThreadPool != null){
            sendTaskThreadPool.shutdown();
        }
    }

    @Override
    public void run() {
        LogUtil.log("port : " + port);

        try {
            if(port > 0){
                socket = new DatagramSocket(port);
            }else{
                socket = new DatagramSocket();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // 创建发送任务线程池
        sendTaskThreadPool = Executors.newFixedThreadPool(1);

        isRunning.set(true);
        while(isRunning.get()){
            byte buf[] = new byte[BUF_SIZE];
            DatagramPacket  packet = new DatagramPacket(buf , buf.length);
            try {
                LogUtil.log("wait receive data...");
                socket.receive(packet);
                final int len = packet.getLength();
                LogUtil.log(packet.getAddress().getHostAddress() + " receive data size : " + len);
                //do callback
                if(callback != null){
                    byte[] receivedData = new byte[len];
                    System.arraycopy(buf , 0 , receivedData , 0, len);
                    callback.onReceiveData(receivedData);
                }
            } catch (IOException e) {
                // e.printStackTrace();
                LogUtil.log("socket been close by other thread!");
            }
        }//end while

        if(!socket.isClosed()){
            socket.close();
        }
        LogUtil.log("socket closed!");
    }
}
