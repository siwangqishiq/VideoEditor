package panyi.xyz.videoeditor.view.widget;

public class Camera {
    public float x;
    public float y;
    public float viewWidth;
    private float viewHeight;

    private float[] mMatrix = new float[3 * 3];
    private float[] result = new float[2];

    public Camera(float x, float y, float width, float height) {
        this.viewWidth = width;
        this.viewHeight = height;
        this.x = x;
        this.y = y;
        reset();
    }

    public void moveTo(float x , float y){
        this.x = x;
        this.y = y;
        reset();
    }

    public void moveBy(float dx , float dy){
        this.x += dx;
        this.y += dy;
        reset();
    }


    private void reset() {
        mMatrix[0] = 2 / viewWidth;
        mMatrix[1] = 0f;
        mMatrix[2] = 0f;

        mMatrix[3] = 0f;
        mMatrix[4] = 2/ viewHeight;
        mMatrix[5] = 0;

        mMatrix[6] = (-2 * x) / viewWidth - 1;
        mMatrix[7] = (-2 * y) / viewHeight - 1;
        mMatrix[8] = 1;
    }

    /**
     *  获得转换变化矩阵
     * @return
     */
    public float[] getMatrix(){
        return mMatrix;
    }

    public float[] worldToScreen(float _x ,float _y){
        result[0] = 2 *(_x - x) / viewWidth - 1.0f;
        result[1] = 2 *(_y - y) / viewHeight - 1.0f;
        return result;
    }
}
