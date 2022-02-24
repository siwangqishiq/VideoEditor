package panyi.xyz.videoeditor.math;

public class Matrix {
    public static void multi(float[] result  ,float[] leftMatrix , float[] rightMatrix){
        result[0] = leftMatrix[0] * rightMatrix[0] + leftMatrix[1] * rightMatrix[3] + leftMatrix[2] * rightMatrix[6];
        result[1] = leftMatrix[0] * rightMatrix[1] + leftMatrix[1] * rightMatrix[4] + leftMatrix[2] * rightMatrix[7];
        result[2] = leftMatrix[0] * rightMatrix[2] + leftMatrix[1] * rightMatrix[5] + leftMatrix[2] * rightMatrix[8];

        result[3] = leftMatrix[3] * rightMatrix[0] + leftMatrix[4] * rightMatrix[3] + leftMatrix[5] * rightMatrix[6];
        result[4] = leftMatrix[3] * rightMatrix[1] + leftMatrix[4] * rightMatrix[4] + leftMatrix[5] * rightMatrix[7];
        result[5] = leftMatrix[3] * rightMatrix[2] + leftMatrix[4] * rightMatrix[5] + leftMatrix[5] * rightMatrix[8];

        result[6] = leftMatrix[6] * rightMatrix[0] + leftMatrix[7] * rightMatrix[3] + leftMatrix[8] * rightMatrix[6];
        result[7] = leftMatrix[6] * rightMatrix[1] + leftMatrix[7] * rightMatrix[4] + leftMatrix[8] * rightMatrix[7];
        result[8] = leftMatrix[6] * rightMatrix[2] + leftMatrix[7] * rightMatrix[5] + leftMatrix[8] * rightMatrix[8];
    }

    public static void vecMultiMatrix(float[] result , float vec[] , float matrix[]){
        result[0] = vec[0] * matrix[0] + vec[1] * matrix[3] + vec[2] * matrix[6];
        result[1] = vec[0] * matrix[1] + vec[1] * matrix[4] + vec[2] * matrix[7];
        result[2] = vec[0] * matrix[2] + vec[1] * matrix[5] + vec[2] * matrix[8];
    }
}
