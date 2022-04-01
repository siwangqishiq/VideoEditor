#version 300 es

#extension GL_OES_EGL_image_external_essl3 : require

precision mediump float;

uniform samplerExternalOES videoTexture;

in vec2 vUv;
out vec4 out_color;

void main(){
//    out_color = texture(sTexture , vUv);
    out_color = vec4(vUv.x, 1.0, 1.0 , 1.0);
}

