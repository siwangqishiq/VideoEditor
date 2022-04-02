#version 300 es

uniform mat3 uMatrix;

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aTexture;

out vec2 vUv;
out vec2 coord;

void main(){
    coord = aPos;
    gl_Position = vec4(uMatrix * vec3(aPos.xy, 1.0f) , 1.0f);
    vUv = aTexture;
}
