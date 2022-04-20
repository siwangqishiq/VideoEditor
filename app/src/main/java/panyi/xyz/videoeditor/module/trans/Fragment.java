package panyi.xyz.videoeditor.module.trans;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Fragment {
    public long pckId; //包ID
    public int totalSize; //包的总大小
    public int fragCount; //分片数量
    public int fragSize; //分片大小
    public int fragId; //分片id
    public int what; //自定义what

    @Override
    public String toString() {
        return "Fragment{" +
                "pckId=" + pckId +
                ", totalSize=" + totalSize +
                ", fragCount=" + fragCount +
                ", fragSize=" + fragSize +
                ", fragId=" + fragId +
                ", what=" + what +
                ", dataBufSize=" + dataBuf.length +
                '}';
    }

    public byte[] dataBuf;

    public byte[] toByteArray(){
        ByteBuffer buf = ByteBuffer.allocate(Packet.HEAD_SIZE + dataBuf.length);
        buf.position(0);

        buf.putLong(pckId);
        buf.putInt(totalSize)
            .putInt(fragCount)
            .putInt(fragSize)
            .putInt(fragId)
            .putInt(what);

        buf.put(dataBuf);

        buf.flip();
        buf.position(0);
        final byte result[] = new byte[buf.remaining()];
        buf.get(result);
        return result;
    }

    public static Fragment decode(byte[] originData,int offset , int length){
        if(originData == null || length < Packet.HEAD_SIZE){
            return null;
        }

        final Fragment frag = new Fragment();

        ByteBuffer buf = ByteBuffer.wrap(originData , offset , length);

        frag.pckId = buf.getLong();
        frag.totalSize = buf.getInt();
        frag.fragCount = buf.getInt();
        frag.fragSize = buf.getInt();
        frag.fragId = buf.getInt();
        frag.what = buf.getInt();

        frag.dataBuf = new byte[frag.fragSize];
        buf.get(frag.dataBuf);
        return frag;
    }
}
