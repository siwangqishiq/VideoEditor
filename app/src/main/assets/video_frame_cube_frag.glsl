#version 300 es

precision mediump float;

uniform sampler2D imageTexture;

in vec2 vUv;

out vec4 out_color;

void main(){
//     out_color = vec4(1.0 , 0.0 ,0.0 , 1.0);
    out_color = texture(imageTexture , vUv);
}

