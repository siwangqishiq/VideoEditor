package panyi.xyz.videoeditor.module.trans;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import panyi.xyz.videoeditor.module.trans.ITrans;
import panyi.xyz.videoeditor.util.LogUtil;

/**
 * 基于UDP实现的传输
 */
public class UdpTrans extends  Thread implements ITrans {
    private int port = -1;

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private DatagramSocket socket;

    private OnReceiveCallback callback;
    private ExecutorService sendTaskThreadPool;

    private byte[] receiveBuf;
    private Map<Long , Packet> receivePackMap = new HashMap<Long,Packet>();

    @Override
    public void startServer(int port, OnReceiveCallback callback) {
        this.port = port;
        this.callback = callback;

        //启动网络线程
        start();
    }

    @Override
    public int sendData(String remoteAddress, int remotePort,int what,  byte[] data) throws UnknownHostException {
        if(socket == null || !isRunning.get() || sendTaskThreadPool == null){
            return -1;
        }

        sendTaskThreadPool.submit(()->{
            //分包
            List<Fragment> fragmentList = Packet.sliceData(data , what);

            for(Fragment frag : fragmentList){
                LogUtil.log("send frag: " + frag);
                sendByUdpTrans(remoteAddress , remotePort , frag.toByteArray());
            }//end for each
        });
        return 0;
    }

    private void sendByUdpTrans(String remoteAddress, int remotePort ,byte[] sendData){
        try {
            DatagramPacket sendPacket = new DatagramPacket(sendData , 0 , sendData.length);
            sendPacket.setPort(remotePort);
            sendPacket.setAddress(InetAddress.getByName(remoteAddress));
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() {
        isRunning.set(false);
        receivePackMap.clear();

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
            if(receiveBuf == null){
                receiveBuf = new byte[Packet.FRAGMENT_SIZE];
            }
            try {
//                LogUtil.log("wait receive data...");
                DatagramPacket  pack = new DatagramPacket(receiveBuf , receiveBuf.length);
                socket.receive(pack);
                final int len = pack.getLength();

                final Fragment frag = Fragment.decode(receiveBuf , 0 , len);
                LogUtil.log("frag : " + frag);

                Packet packet = receivePackMap.get(frag.pckId);
                if(packet == null){
                    packet = new Packet(frag.pckId , frag.fragCount , frag.totalSize , frag.what);
                    packet.addFragment(frag);

                    receivePackMap.put(packet.getPckId() , packet);
                }else{
                    packet.addFragment(frag);
                }

                if(packet.checkPacketComplete()){ //完整
                    //do callback
                    if(callback != null){
                        final byte[] dataBuf = packet.extractData();
                        callback.onReceiveData(packet.getWhat() , dataBuf);
                    }
                    receivePackMap.remove(packet.getPckId());
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
