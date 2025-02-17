float hash13(vec3 p3) {
    p3 = fract(p3 * .1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float cubidNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    float aaa = hash13(i + vec3(0., 0., 0.));
    float aab = hash13(i + vec3(0., 0., 1.));
    float aba = hash13(i + vec3(0., 1., 0.));
    float abb = hash13(i + vec3(0., 1., 1.));
    float baa = hash13(i + vec3(1., 0., 0.));
    float bab = hash13(i + vec3(1., 0., 1.));
    float bba = hash13(i + vec3(1., 1., 0.));
    float bbb = hash13(i + vec3(1., 1., 1.));

    float aam = mix(aaa, aab, f.z);
    float abm = mix(aba, abb, f.z);
    float bam = mix(baa, bab, f.z);
    float bbm = mix(bba, bbb, f.z);

    float amm = mix(aam, abm, f.y);
    float bmm = mix(bam, bbm, f.y);

    return mix(amm, bmm, f.x);
}

float fbm3(vec3 p, int layers) {
    float z=2.;
    float rz = 0.;
    for (int i=0;i<layers;i++) {
        rz += abs((cubidNoise(p)-0.5)*2.)/z;
        z = z*2.;
        p = p*2.;
    }
    return rz;
}

float magicEnergyEffect(vec3 pos) {
    //get two rotated fbm calls and displace the domain
    vec3 p2 = pos*.7;
    float t = time * .5;
    vec3 basis = vec3(fbm3(p2-t*1.6, 2), fbm3(p2+t*1.7, 2), fbm3(p2+t*1.5, 2));
    basis = (basis-.5)*.35;
    pos += basis;

    return fbm3(pos, 3);
}