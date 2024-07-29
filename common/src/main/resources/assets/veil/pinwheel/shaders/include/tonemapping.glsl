// Copyright (C) 2019 Damien Seguin
// https://github.com/dmnsgn/glsl-tone-map
// Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
vec3 aces_min(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return (x * (a * x + b)) / (x * (c * x + d) + e);
}

vec3 aces_min_inv(vec3 x) {
//    return (sqrt(-10127.*x*x + 13702.*x + 9.) + 59.*x - 3.) / (502. - 486.*x);
    return (-0.59 * x + 0.03 - sqrt(-1.0127 * x*x + 1.3702 * x + 0.0009)) / (2.0 * (2.43*x - 2.51));
}

// https://www.shadertoy.com/view/XsGfWV
// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
vec3 aces_full(vec3 color) {
    const mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
	);
	const mat3 m2 = mat3(
         1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
	);
	vec3 v = m1 * color;
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return m2 * (a / b);
}

//vec3 aces_full_inv(vec3 color) {
//    const mat3 M = mat3(
//
//    );
//}
