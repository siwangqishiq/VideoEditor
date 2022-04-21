package panyi.xyz.videoeditor.module.trans;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import panyi.xyz.videoeditor.util.UuidUtil;

/**
 * 网络分包 合包操作
 */
public class Packet {
    public static final int BUF_SIZE = 60 * 1024; //60k 实体数据区buf
    public static final int HEAD_SIZE = 8 + 4 + 4 +4 + 4 + 4;// 包ID + 包总大小 + 分片大小+ 分片数量 + 分片ID +包类型
    public static final int FRAGMENT_SIZE = HEAD_SIZE + BUF_SIZE;

    private long pckId = -1;
    private int fragCount = -1;
    private int totalSize = -1;
    private int what;

    private Map<Integer , Fragment> fragMap = new HashMap<Integer , Fragment>();

    @Override
    public String toString() {
        return "Packet{" +
                "pckId=" + pckId +
                ", p( " + fragMap.size() +
                " / " + fragCount +
                " ) what=" + what +
                '}';
    }

    public Packet(long pckId, int fragCount, int totalSize, int what) {
        this.pckId = pckId;
        this.fragCount = fragCount;
        this.totalSize = totalSize;
        this.what = what;
    }

    public long getPckId() {
        return pckId;
    }

    /**
     * 检测包是否完整
     * @return
     */
    public boolean checkPacketComplete(){
        if(totalSize <0 || fragCount < 0){
            return false;
        }

        for(int i = 0; i < fragCount;i++){
            if(fragMap.get(i) == null){
                return false;
            }
        }

        return true;
    }

    public byte[] extractData(){
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        for(int i = 0; i < fragCount;i++){
            Fragment frag = fragMap.get(i);
            buf.put(frag.dataBuf);
        }//end for i

        buf.flip();
        byte retBuf[] = new byte[buf.remaining()];
        buf.get(retBuf);
        return retBuf;
    }

    public byte[] extractDataWithError(){
        ByteBuffer buf = ByteBuffer.allocate(totalSize);

        for(int i = 0; i < fragCount;i++){
            Fragment frag = fragMap.get(i);
            if(frag == null){

            }else{
                buf.put(frag.dataBuf);
            }
        }//end for i

        buf.flip();
        byte retBuf[] = new byte[buf.remaining()];
        buf.get(retBuf);
        return retBuf;
    }

    public void addFragment(final Fragment fragment){
        if(fragment == null){
            return;
        }

        fragMap.put(fragment.fragId , fragment);
    }

    public int getWhat() {
        return what;
    }

    public static List<Fragment> sliceData(byte[] data , int what){
        if(data == null || data.length == 0){
            return null;
        }

        final long packId = UuidUtil.genUniqueId();

        int fragCount = findFragCount(data.length , BUF_SIZE);
        List<Fragment> fragList = new ArrayList<Fragment>(fragCount);
        int offset = 0;
        int fragIndex = 0;
        while(offset < data.length){
            final int size = Math.min( data.length - offset , BUF_SIZE);
            final Fragment fragment = new Fragment();
            fragment.pckId = packId;
            fragment.totalSize = data.length;
            fragment.fragCount = fragCount;
            fragment.fragSize = size;
            fragment.fragId = fragIndex;
            fragment.what = what;

            fragment.dataBuf = new byte[fragment.fragSize];
            System.arraycopy(data , offset , fragment.dataBuf , 0 , fragment.fragSize);

            fragList.add(fragment);
            fragIndex++;
            offset += size;
        }//end while
        return fragList;
    }

    private static int findFragCount(int dataSize , int fragSize){
        if(dataSize % fragSize == 0){
            return dataSize/ fragSize;
        }else{
            return dataSize/ fragSize + 1;
        }
    }
}
