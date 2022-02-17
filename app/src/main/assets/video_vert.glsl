#version 300 es

// uniform mat4 uMVPMatrix; //总变换矩阵
uniform mat4 uSTMatrix; //纹理变换矩阵

layout(location = 0) in vec2 aPosition;  //顶点位置
layout(location = 1) in vec2 aTextureCoord;    //纹理坐标

out vec2 vTextureCoord;

void main()  {

    //uMVPMatrix *
    gl_Position = vec4(aPosition.xy  , 0 , 1);
    //    gl_Position = vec4(aPosition.xy  , 0 , 1);
    vTextureCoord = (uSTMatrix * vec4(aTextureCoord.xy , 0 , 1)).st;
//    vTextureCoord = aTextureCoord.xy;
}