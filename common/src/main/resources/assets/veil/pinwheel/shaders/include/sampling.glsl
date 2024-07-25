// https://github.com/Unity-Technologies/PostProcessing/blob/v2/PostProcessing/Shaders/Sampling.hlsl

// Better, temporally stable box filtering
// [Jimenez14] http://goo.gl/eomGso
// . . . . . . .
// . A . B . C .
// . . D . E . .
// . F . G . H .
// . . I . J . .
// . K . L . M .
// . . . . . . .
vec4 DownsampleBox13Tap(sampler2D tex, vec2 uv, vec2 texelSize) {
    vec4 A = texture(tex, uv + texelSize * vec2(-1., -1.));
    vec4 B = texture(tex, uv + texelSize * vec2( 0., -1.));
    vec4 C = texture(tex, uv + texelSize * vec2( 1., -1.));
    vec4 D = texture(tex, uv + texelSize * vec2(-.5, -.5));
    vec4 E = texture(tex, uv + texelSize * vec2( .5, -.5));
    vec4 F = texture(tex, uv + texelSize * vec2(-1.,  0.));
    vec4 G = texture(tex, uv                             );
    vec4 H = texture(tex, uv + texelSize * vec2( 1.,  0.));
    vec4 I = texture(tex, uv + texelSize * vec2(-.5,  .5));
    vec4 J = texture(tex, uv + texelSize * vec2( .5,  .5));
    vec4 K = texture(tex, uv + texelSize * vec2(-1.,  1.));
    vec4 L = texture(tex, uv + texelSize * vec2( 0.,  1.));
    vec4 M = texture(tex, uv + texelSize * vec2( 1.,  1.));

    vec2 div = .25 * vec2(.5, .125);

    vec4 o = (D + E + I + J) * div.x;
    o += (A + B + G + F) * div.y;
    o += (B + C + H + G) * div.y;
    o += (F + G + L + K) * div.y;
    o += (G + H + M + L) * div.y;

    return o;
}

// Standard box filtering
vec4 DownsampleBox4Tap(sampler2D tex, vec2 uv, vec2 texelSize) {
    vec4 d = texelSize.xyxy * vec4(-1., -1., 1., 1.);
    
    vec4 s;
    s =  (texture(tex, uv + d.xy));
    s += (texture(tex, uv + d.zy));
    s += (texture(tex, uv + d.xw));
    s += (texture(tex, uv + d.zw));
    
    return s * .25;
}

// 9-tap bilinear upsampler (tent filter)
vec4 UpsampleTent(sampler2D tex, vec2 uv, vec2 texelSize, vec4 sampleScale) {
    vec4 d = texelSize.xyxy * vec4(1., 1., -1., 0.) * sampleScale;
    
    vec4 s;
    s =  texture(tex, uv - d.xy);
    s += texture(tex, uv - d.wy) * 2.;
    s += texture(tex, uv - d.zy);
    
    s += texture(tex, uv + d.zw) * 2.;
    s += texture(tex, uv       ) * 4.;
    s += texture(tex, uv + d.xw) * 2.;
    
    s += texture(tex, uv + d.zy);
    s += texture(tex, uv + d.wy) * 2.;
    s += texture(tex, uv + d.xy);
    
    return s * .0625;
}

// Standard box filtering
vec4 UpsampleBox(sampler2D tex, vec2 uv, vec2 texelSize, vec4 sampleScale) {
    vec4 d = texelSize.xyxy * vec4(-1., -1., 1., 1.) * (sampleScale * .5);
    
    vec4 s;
    s =  (texture(tex, uv + d.xy));
    s += (texture(tex, uv + d.zy));
    s += (texture(tex, uv + d.xw));
    s += (texture(tex, uv + d.zw));
    
    return s * .25;
}
