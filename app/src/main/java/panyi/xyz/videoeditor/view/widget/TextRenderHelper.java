package panyi.xyz.videoeditor.view.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import panyi.xyz.videoeditor.util.LogUtil;
import panyi.xyz.videoeditor.util.ShaderUtil;

/**
 *  文字渲染辅助类
 */
public class TextRenderHelper {
    private static final String ALL_CHAR = "你好世界 " +
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890:";

    private static final int TEXTURE_SIZE = 1024;
    private static final int MAX_ROW = 64;

    private Context context;

    public static class CharRenderInfo{
        char ch; //代表字符
        int textureId;//纹理ID
        //纹理 uv 坐标
        float u;
        float v;
        //右上角纹理
        float ux;
        float uy;
    }

    private Map<Character , CharRenderInfo> charInfo = new HashMap<Character , CharRenderInfo>();

    private float viewWidth;
    private float viewHeight;

    private int bitTextureId = -1;

    private int textProgramId = -1;

    public TextRenderHelper(Context context , float w , float h){
        this.context = context;

        viewWidth = w;
        viewHeight = h;

        createTexture();
        initVertexAttribute();
        initShader();
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

    }

    private void initShader(){
        textProgramId = ShaderUtil.buildShaderProgramFromAssets(context ,
                "text_vert.glsl",
                "text_frag.glsl");
        LogUtil.log("text render programid : " + textProgramId);
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

    }

    public void free(){
        int ids[] = new int[1];
        ids[0] = bitTextureId;
        GLES30.glDeleteTextures(1 , ids , 0);
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
        info.uy = uy;
        charInfo.put(ch , info);
        // LogUtil.log(ch + "  " + "u-v " + info.u + "- "+ info.v +"  ux-uy " + info.ux +" - " + info.uy);
    }
}
