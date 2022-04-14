#version 300 es

precision mediump float;

uniform sampler2D textTexture;

in vec2 vUv;
out vec4 out_color;

void main(){
    vec4 textColor = vec4(1.0 , 0.0 , 0.0f , 0.6f);
    float value = texture(textTexture , vUv).a;
    out_color = vec4(textColor.xyz , textColor.a * value);
//    out_color =vec4(1.0f , 1.0f ,1.0f  ,0.0f);
}

