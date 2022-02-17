#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require

precision highp float;

in vec2 vTextureCoord;

uniform int uEffectType;
uniform vec2 uTouchPoint;
uniform vec2 uTextureSize;
uniform int uMosicSize;

uniform samplerExternalOES sTexture;
out vec4 frag_color;

//灰度
vec4 effectGray(vec4 color){
    vec4 result;

    if(gl_FragCoord.x <= uTouchPoint.x){
        float grayVal = color.r*0.299 + color.g*0.587 + color.b*0.114;

        result.r =  grayVal;
        result.g = grayVal;
        result.b = grayVal;
        result.a = 1.0;
    }else{
        result = color;
    }
    return result;
}

//马赛克
vec4 effectMosaic(vec4 color) {
    if(gl_FragCoord.x > uTouchPoint.x){
        return color;
    }

    float s_w = uTextureSize.x;
    float s_h = uTextureSize.y;

    float w_cube = 1.0 / s_w;
    float h_cube  = 1.0/ s_h;

    vec4 result = vec4(0 , 0 , 0, 1.0);

    float mosicWidth = w_cube * float(uMosicSize);
    float mosicHeight = h_cube * float(uMosicSize);

    float x = 0.0;
    float x_offset = 0.0;
    while(x < 1.0){
        x_offset = x + mosicWidth;
        if(x_offset > vTextureCoord.x){
            break;
        }
        x = x_offset;
    }//end while

    float y = 0.0;
    float y_offset = 0.0;
    while(y < 1.0){
        y_offset = y + mosicHeight;
        if(y_offset > vTextureCoord.y){
            break;
        }
        y = y_offset;
    }//end while

    //result = texture(sTexture, vec2(x , y)).rgba;

    int totoal_size = 0;
    for(;x<x_offset ; x+= w_cube){
        for(;y < y_offset ; y+= h_cube){
            vec4 cur_color = texture(sTexture, vec2(x , y)).rgba;

            result.r += cur_color.r;
            result.g += cur_color.g;
            result.b += cur_color.b;

            totoal_size++;
        }
    }
    result.r =  result.r / float(totoal_size);
    result.g = result.g / float(totoal_size);
    result.b = result.b / float(totoal_size);

    return result;
}

// 正态分布概率密度函数
float normpdf(float x, float sigma) {
    return 0.39894*exp(-0.5*x*x/(sigma*sigma))/sigma;
}

//高斯模糊
vec4 effectGauss(vec4 color) {
    if(gl_FragCoord.x > uTouchPoint.x){
        return color;
    }

    float s_w = uTextureSize.x;
    float s_h = uTextureSize.y;

    float w_cube = 1.0 / s_w;
    float h_cube = 1.0 / s_h;

    vec4 sum = vec4(0.0);

    sum += texture(sTexture, vec2(vTextureCoord.x - 8.0*w_cube, vTextureCoord.y - 8.0*h_cube)) * 0.003799;
    sum += texture(sTexture, vec2(vTextureCoord.x - 7.0*w_cube, vTextureCoord.y - 8.0*h_cube)) * 0.003799;
    sum += texture(sTexture, vec2(vTextureCoord.x - 7.0*w_cube, vTextureCoord.y - 7.0*h_cube)) * 0.008741;
    sum += texture(sTexture, vec2(vTextureCoord.x - 6.0*w_cube, vTextureCoord.y - 6.0*h_cube)) * 0.017997;
    sum += texture(sTexture, vec2(vTextureCoord.x - 5.0*w_cube, vTextureCoord.y - 5.0*h_cube)) * 0.033159;
    sum += texture(sTexture, vec2(vTextureCoord.x - 4.0*w_cube, vTextureCoord.y - 4.0*h_cube)) * 0.054670;
    sum += texture(sTexture, vec2(vTextureCoord.x - 3.0*w_cube, vTextureCoord.y - 3.0*h_cube)) * 0.080657;
    sum += texture(sTexture, vec2(vTextureCoord.x - 2.0*w_cube, vTextureCoord.y - 2.0*h_cube)) * 0.106483;
    sum += texture(sTexture, vec2(vTextureCoord.x - 1.0*w_cube, vTextureCoord.y - 1.0*h_cube)) * 0.125794;
    sum += texture(sTexture, vec2(vTextureCoord.x, vTextureCoord.y)) * 0.137401;
    sum += texture(sTexture, vec2(vTextureCoord.x + 1.0*w_cube, vTextureCoord.y + 1.0*h_cube)) * 0.125794;
    sum += texture(sTexture, vec2(vTextureCoord.x + 2.0*w_cube, vTextureCoord.y + 2.0*h_cube)) * 0.106483;
    sum += texture(sTexture, vec2(vTextureCoord.x + 3.0*w_cube, vTextureCoord.y + 3.0*h_cube)) * 0.080657;
    sum += texture(sTexture, vec2(vTextureCoord.x + 4.0*w_cube, vTextureCoord.y + 4.0*h_cube)) * 0.054670;
    sum += texture(sTexture, vec2(vTextureCoord.x + 5.0*w_cube, vTextureCoord.y + 5.0*h_cube)) * 0.033159;
    sum += texture(sTexture, vec2(vTextureCoord.x + 6.0*w_cube, vTextureCoord.y + 6.0*h_cube)) * 0.017997;
    sum += texture(sTexture, vec2(vTextureCoord.x + 7.0*w_cube, vTextureCoord.y + 7.0*h_cube)) * 0.008741;

    return sum;
}

vec4 blur(vec4 color) {
    if(gl_FragCoord.x > uTouchPoint.x){
        return color;
    }

    float disp = 0.;
    float intensity =0.2 ;
    const int passes = 6;
    vec4 c1 = vec4(0.0);
    disp = intensity*(0.5-distance(0.5, 0.1));

    for (int xi=0; xi<passes; xi++) {
        float x = float(xi) / float(passes) - 0.5;
        for (int yi=0; yi<passes; yi++) {
            float y = float(yi) / float(passes) - 0.5;
            vec2 v = vec2(x, y);
            float d = disp;
            c1 += texture(sTexture, vTextureCoord + d*v);
        }
    }
    c1 /= float(passes*passes);
    return c1;
}


void main() {
    vec4 originColor = texture(sTexture, vTextureCoord).rgba;
    switch(uEffectType){
        case 0://正常
        frag_color = originColor;
        break;
        case 1://灰度
        frag_color = effectGray(originColor);
        break;
        case 2://马赛克
        frag_color = effectMosaic(originColor);
        break;
        case 3://模糊
        frag_color = effectGauss(originColor);
//        frag_color = blur(originColor);
        break;
        default:
        frag_color = originColor;
        break;
    }
}