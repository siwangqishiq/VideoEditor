#version 300 es

precision mediump float;

uniform sampler2D textTexture;

in vec2 vUv;
out vec4 out_color;

void main(){
//    vec4 origin = texture(videoTexture , vUv).xyzw;
//    out_color = vec4(origin.xyz , 1.0f);
    out_color =vec4(1.0f , 0.0f ,1.0f  ,1.0f);
}

