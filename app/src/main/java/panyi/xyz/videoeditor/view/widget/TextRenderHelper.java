package panyi.xyz.videoeditor.view.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;

/**
 *  文字渲染辅助类
 */
public class TextRenderHelper {
    private static final String ALL_CHAR = "你好世界 死 包夜" +
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:";

    private static final int TEXTURE_SIZE = 1024;
    private static final int MAX_COUNT_PER_LINE = 64;

    private Context context;

    public static class CharRenderInfo{
        char ch; //代表字符
        int textureId;//纹理ID
        //纹理 uv 坐标
        float u;
        float v;
        //右上角纹理
        float ux;
        float vy;
    }

    private Map<Character , CharRenderInfo> charInfo = new HashMap<Character , CharRenderInfo>();
    private Camera camera;

    private int bitTextureId = -1;

    private int textProgramId = -1;

    private int uMatrixLoc = -1;

    private int posBufId;
    private FloatBuffer posBuf;

    private int texBufId;
    private FloatBuffer texBuf;

    public TextRenderHelper(Context context , float w , float h){
        this.context = context;
        camera = new Camera(0 ,0 , w , h);

        createTexture();
        initVertexAttribute();
        initShader();

        //开启blend混合方式  以正确渲染半透明的图片
//        GLES30.glEnable(GLES30.GL_BLEND);
    }

    private void createTexture(){
        int ids[] = new int[1];
        GLES30.glGenTextures(1 , ids , 0);
        bitTextureId = ids[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bitTextureId);

        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        Bitmap genBit = buildFontBit(bitTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D , 0 , genBit , 0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        genBit.recycle();
    }

    /**
     * 创建关联顶点数据
     */
    private void initVertexAttribute(){
        int bufIds[] = new int[2];
        GLES30.glGenBuffers(2 , bufIds , 0);

        posBufId = bufIds[0];
        texBufId = bufIds[1];

        posBuf = ByteBuffer.allocateDirect(bufSize())
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
//        for(int i = 0 ; i < bufSize();i++){
//            posBuf.put(0.0f);
//        }//end for each
        posBuf.position(0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , bufSize() , posBuf , GLES30.GL_DYNAMIC_DRAW);

        texBuf =  ByteBuffer.allocateDirect(bufSize())
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
//        for(int i = 0 ; i < bufSize();i++){
//            texBuf.put(0.0f);
//        }//end for each
        texBuf.position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , texBufId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , bufSize() , texBuf , GLES30.GL_DYNAMIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , 0);
    }

    /**
     * 每个字 6个顶点 每个顶点 2个浮点型
     * @return
     */
    private static  int bufSize(){
        return 2 * 6 * MAX_COUNT_PER_LINE * Float.BYTES;
    }

    private void initShader(){
        textProgramId = ShaderUtil.buildShaderProgramFromAssets(context ,
                "text_vert.glsl",
                "text_frag.glsl");
        LogUtil.log("text render programid : " + textProgramId);

        uMatrixLoc = GLES30.glGetUniformLocation(textProgramId , "uMatrix");
        LogUtil.log("text render uMatrixLoc : " + uMatrixLoc);
    }


    /**
     *  渲染文字 x/y 坐标 为文字左下角坐标
     *
     * @param text
     * @param x
     * @param y
     * @param textSize
     */
    public void renderText(String text , float x , float y , float textSize){
        if(text == "" || text == null){
            return;
        }

        int vertexCount = fillBuf(text , x , y , textSize);
        doRender(vertexCount);
    }

    /**
     *
     * @param text
     * @param initX
     * @param initY
     * @param size
     * @return 顶点数量
     *
     */
    private int fillBuf(final String text , float initX , float initY , float size){
        float x = initX;
        float y = initY;

        posBuf.position(0);
        texBuf.position(0);

        int count = 0;
        for(int i = 0; i < Math.min(text.length() , MAX_COUNT_PER_LINE); i++){
            char ch = text.charAt(i);

            final CharRenderInfo renderInfo = charInfo.get(ch);
            float chWidth = size / 2.0f;

            if(renderInfo != null){
                chWidth = calculateCharWidth(renderInfo , size);
                fillNormalBuf(x , y , chWidth ,size , renderInfo);
            }else{
                fillEmptyBuf(x,  y , size);
            }

            x += chWidth;
            count += 6;
        }//end for i
        return count;
    }

    private float calculateCharWidth(CharRenderInfo renderInfo , float textSize){
        float w = renderInfo.ux - renderInfo.u;
        float h = Math.abs(renderInfo.vy - renderInfo.v);
        if(h != 0.0f){
            float ratio = w/ h;
            return ratio * textSize;
        }else{
            return textSize / 2.0f;
        }
    }

    private void fillNormalBuf(float x , float y ,float chWidth,  float chHeight , final CharRenderInfo renderInfo){
        posBuf.put(x);
        posBuf.put(y);

        posBuf.put(x +chWidth);
        posBuf.put(y);

        posBuf.put(x + chWidth);
        posBuf.put(y + chHeight);

        posBuf.put(x);
        posBuf.put(y);

        posBuf.put(x + chWidth);
        posBuf.put(y + chHeight);

        posBuf.put(x);
        posBuf.put(y + chHeight);

        texBuf.put(renderInfo.u);
        texBuf.put(renderInfo.v);

        texBuf.put(renderInfo.ux);
        texBuf.put(renderInfo.v);

        texBuf.put(renderInfo.ux);
        texBuf.put(renderInfo.vy);

        texBuf.put(renderInfo.u);
        texBuf.put(renderInfo.v);

        texBuf.put(renderInfo.ux);
        texBuf.put(renderInfo.vy);

        texBuf.put(renderInfo.u);
        texBuf.put(renderInfo.vy);
    }

    private void fillEmptyBuf(float x , float y , float size){
        float w = size / 2.0f;
        float h = size;

        posBuf.put(x);
        posBuf.put(y);

        posBuf.put(x +w);
        posBuf.put(y);

        posBuf.put(x + w);
        posBuf.put(y + h);

        posBuf.put(x);
        posBuf.put(y);

        posBuf.put(x + w);
        posBuf.put(y + h);

        posBuf.put(x);
        posBuf.put(y + h);

        texBuf.put(0);
        texBuf.put(0);

        texBuf.put(0);
        texBuf.put(0);

        texBuf.put(0);
        texBuf.put(0);

        texBuf.put(0);
        texBuf.put(0);

        texBuf.put(0);
        texBuf.put(0);

        texBuf.put(0);
        texBuf.put(0);
    }

    private void doRender(final int vertexCount){
        GLES30.glUseProgram(textProgramId);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , posBufId);
        posBuf.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , vertexCount * 2 * Float.BYTES ,
                posBuf , GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(0, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER , texBufId);
        texBuf.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER , vertexCount * 2 * Float.BYTES ,
                texBuf , GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(1, 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , 0);

        GLES30.glUniformMatrix3fv(uMatrixLoc , 1 , false ,
                camera.getMatrix(), 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D , bitTextureId);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES , 0 , vertexCount);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D , 0);
    }

    public void free(){
        int ids[] = new int[1];
        ids[0] = bitTextureId;
        GLES30.glDeleteTextures(1 , ids , 0);

        if(textProgramId >= 0 ){
            GLES30.glDeleteProgram(textProgramId);
        }

        int bufIds[] = new int[2];
        bufIds[0] = posBufId;
        bufIds[1] = texBufId;
        GLES30.glDeleteBuffers(2 , bufIds , 0);
    }

    /**
     * 创建字体Bitmap
     * 后续送入GPU纹理
     * @return
     */
    public Bitmap buildFontBit(final int textureId){
        final String content = ALL_CHAR;

        Paint paint = new Paint();
        paint.setTextSize(128.0f);
        paint.setColor(Color.BLACK);

        final float width = TEXTURE_SIZE;
        final float height = TEXTURE_SIZE;

        final float radioX = 1.0f / width;
        final float radioY = 1.0f / height;

        final Bitmap bitmap = Bitmap.createBitmap((int)width , (int)height , Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint.FontMetrics fm = paint.getFontMetrics();
        float fontHeight = fm.descent - fm.ascent;
        float x = 0;
        float y = 0;
        for(int i = 0 ; i < content.length() ;i++){
            Character ch = content.charAt(i);
            float chWidth = paint.measureText(ch.toString());
            if(x + chWidth > width){
                x = 0;
                y += fontHeight;
            }

            canvas.drawText(ch.toString() , x , y - fm.ascent  ,  paint);

            addCharRenderInfo(ch , textureId , x * radioX, (y + fontHeight) * radioY ,
                    (x + chWidth) * radioX , y * radioY);

            x = x + chWidth;
        }//end for i
        return bitmap;
    }

    private void addCharRenderInfo(char ch , int textureId , float u , float v , float ux , float uy){
        final CharRenderInfo info = new CharRenderInfo();
        info.ch = ch;
        info.textureId = textureId;
        info.u = u;
        info.v = v;
        info.ux = ux;
        info.vy = uy;
        charInfo.put(ch , info);
        // LogUtil.log(ch + "  " + "u-v " + info.u + "- "+ info.v +"  ux-uy " + info.ux +" - " + info.uy);
    }
}
