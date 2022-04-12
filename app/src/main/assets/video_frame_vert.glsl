#version 300 es

uniform mat3 uMatrix;

uniform mat4 uTextureMatrix;

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aTexture;

out vec2 vUv;
out vec2 coord;

void main(){
    coord = aPos;
    gl_Position = vec4(uMatrix * vec3(aPos.xy, 1.0f) , 1.0f);
//    vUv = (uTextureMatrix * vec4(aTexture , 1.0 , 1.0)).xy;
//    gl_Position = vec4(aPosition.xy , 1.0f , 1.0f);
    vUv = vec2(aTexture.x , 1.0f - aTexture.y);
}
