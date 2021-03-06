#version 300 es

uniform mat3 uMatrix;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexture;

out vec2 vUv;

void main(){
     gl_Position = vec4(uMatrix * vec3(aPosition.xy, 1.0f) , 1.0f);
//     vUv = vec2(aTexture.x , 1.0 - aTexture.y);
     vUv = vec2(aTexture.x , aTexture.y);
}
