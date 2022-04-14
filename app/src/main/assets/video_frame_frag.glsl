#version 300 es

#extension GL_OES_EGL_image_external_essl3 : require

precision mediump float;

uniform samplerExternalOES videoTexture;

in vec2 coord;
in vec2 vUv;
out vec4 out_color;

void main(){
//    const float storken = 20.0f;
//    if(abs(coord.x - coord.y) <= storken || abs(coord.y + coord.x - 400.0f) <= storken){
//        out_color = vec4(0.0f , 0.0f ,0.0f, 1.0f);
//    }else{
//        vec3 origin = texture(videoTexture , vUv).xyz;
//        out_color = vec4(origin.x , origin.y ,origin.z , 1.0f);
//    }

    vec4 origin = texture(videoTexture , vUv).xyzw;
    out_color = vec4(origin.xyz , 1.0f);

//    out_color =vec4(1.0f , 0.0f ,0.0f  ,1.0f);
}

