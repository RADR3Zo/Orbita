#version 150

// Cielo procedural de Nova Client: se calcula en la tarjeta grafica para cada pixel (nitido a cualquier resolucion).
// NovaParams: x = estilo (0 nebulosa, 1 aurora, 2 celdas, 3 estrellas, 4 degradado), y = brillo, z = giro (radianes)

uniform vec4 ColorModulator;
uniform float NovaTime;
uniform vec3 NovaColor;
uniform vec3 NovaColor2;
uniform vec4 NovaParams;

in vec3 skyDir;
out vec4 fragColor;

vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)), dot(p, vec3(269.5, 183.3, 246.1)), dot(p, vec3(113.5, 271.9, 124.6)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float hash1(vec3 p) { return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453); }

// ruido de gradiente con interpolacion de quinto grado: suave, sin cuadriculas
float gnoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = dot(hash3(i), f);
    float b = dot(hash3(i + vec3(1, 0, 0)), f - vec3(1, 0, 0));
    float c = dot(hash3(i + vec3(0, 1, 0)), f - vec3(0, 1, 0));
    float d = dot(hash3(i + vec3(1, 1, 0)), f - vec3(1, 1, 0));
    float e = dot(hash3(i + vec3(0, 0, 1)), f - vec3(0, 0, 1));
    float g = dot(hash3(i + vec3(1, 0, 1)), f - vec3(1, 0, 1));
    float h = dot(hash3(i + vec3(0, 1, 1)), f - vec3(0, 1, 1));
    float k = dot(hash3(i + vec3(1, 1, 1)), f - vec3(1, 1, 1));
    return mix(mix(mix(a, b, u.x), mix(c, d, u.x), u.y), mix(mix(e, g, u.x), mix(h, k, u.x), u.y), u.z);
}

const mat3 ROT = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

float fbm(vec3 p, int oct) {
    float s = 0.0, a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (i >= oct) break;
        s += a * gnoise(p);
        p = ROT * p * 2.02;
        a *= 0.5;
    }
    return s; // aprox. -0.5 .. 0.5
}

// estrellas pequeñas con borde suave y titileo
float stars(vec3 d, float density) {
    vec3 p = d * 95.0;
    vec3 i = floor(p);
    float h = hash1(i);
    if (h < 1.0 - density) return 0.0;
    vec3 off = vec3(hash1(i + 3.1), hash1(i + 7.7), hash1(i + 1.3)) * 0.6 + 0.2;
    float dist = length(fract(p) - off);
    float size = 0.08 + 0.14 * fract(h * 37.0);
    float tw = 0.65 + 0.35 * sin(NovaTime * 0.08 + h * 400.0);
    return smoothstep(size, 0.0, dist) * tw * (0.5 + 1.5 * fract(h * 91.0));
}

vec3 nebula(vec3 d, float t) {
    vec3 p = d * 1.8;
    float s = t * 0.004;
    vec3 q = vec3(fbm(p + vec3(0.0, 0.0, s), 4), fbm(p + vec3(5.2, 1.3, -s), 4), fbm(p + vec3(1.7, 9.2, s), 4));
    float n = fbm(p + 2.2 * q, 5) + 0.5;
    float ridge = pow(1.0 - abs(2.0 * n - 1.0), 5.0);
    float cloud = smoothstep(0.38, 0.95, n);
    vec3 tint = mix(NovaColor, NovaColor2, clamp(q.x * 2.0 + 0.5, 0.0, 1.0));
    vec3 col = tint * (ridge * 1.5 + cloud * 0.55) + vec3(1.0) * pow(ridge, 9.0) * 0.6;
    col += NovaColor * 0.03;
    col += vec3(stars(d, 0.02)) * (1.0 - cloud);
    return col;
}

vec3 aurora(vec3 d, float t) {
    float y = d.y;
    vec3 col = mix(vec3(0.01, 0.015, 0.04), vec3(0.03, 0.05, 0.1), clamp(1.0 - y, 0.0, 1.0));
    col += vec3(stars(d, 0.025)) * 0.8;
    if (y > 0.0) {
        vec2 xz = d.xz / (y + 0.3);
        float acc = 0.0;
        vec3 acol = vec3(0.0);
        for (int layer = 0; layer < 3; layer++) {
            float fl = float(layer);
            float w = fbm(vec3(xz * (0.6 + fl * 0.25), t * 0.006 + fl * 3.0), 4);
            float curtain = pow(max(0.0, 1.0 - abs(w * 3.2 - fl * 0.25)), 7.0);
            float streak = 0.55 + 0.45 * gnoise(vec3(xz * 9.0, t * 0.02 + fl));
            float band = smoothstep(0.02, 0.18, y) * (1.0 - smoothstep(0.45 + fl * 0.1, 0.95, y));
            float a = curtain * streak * band;
            acc += a;
            acol += mix(NovaColor2, NovaColor, fl / 2.0) * a;
        }
        col += acol * 1.6 + vec3(0.8, 1.0, 0.9) * pow(acc, 3.0) * 0.15;
    }
    return col;
}

vec3 cells(vec3 d, float t) {
    vec3 p = d * 3.2 + vec3(sin(t * 0.004), 0.0, cos(t * 0.003)) * 0.3;
    vec3 ip = floor(p);
    float f1 = 9.0, f2 = 9.0;
    for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int c = -1; c <= 1; c++) {
        vec3 cell = ip + vec3(a, b, c);
        vec3 h = hash3(cell) * 0.5 + 0.5;
        vec3 pt = cell + 0.5 + 0.38 * sin(t * 0.01 + h * 6.2831);
        float dist = length(p - pt);
        if (dist < f1) { f2 = f1; f1 = dist; } else if (dist < f2) f2 = dist;
    }
    float edge = f2 - f1;
    float aa = fwidth(edge) * 1.5 + 0.004;
    float line = 1.0 - smoothstep(0.0, aa + 0.02, edge);
    float glow = exp(-edge * 10.0);
    vec3 tint = mix(NovaColor, NovaColor2, smoothstep(0.0, 0.8, f1));
    return tint * (glow * 0.9 + line * 1.4) + vec3(1.0) * line * 0.35 + NovaColor * 0.03;
}

vec3 starfield(vec3 d, float t) {
    float band = exp(-pow(dot(d, normalize(vec3(0.3, 0.5, 0.8))) * 3.2, 2.0)); // via lactea
    float neb = pow(fbm(d * 2.5 + vec3(0.0, t * 0.0008, 0.0), 5) + 0.5, 3.0) * band;
    vec3 col = mix(NovaColor, NovaColor2, neb) * neb * 1.4 + vec3(0.005, 0.007, 0.015);
    col += vec3(stars(d, 0.03)) + vec3(stars(d * 1.7, 0.02)) * 0.6 * (0.4 + band);
    return col;
}

vec3 gradientSky(vec3 d, float t) {
    float y = clamp(d.y, -1.0, 1.0);
    float horizon = pow(1.0 - abs(y), 6.0);
    vec3 col = mix(NovaColor2 * 0.08, NovaColor * 0.9, horizon);
    col += NovaColor * pow(horizon, 3.0) * 0.6;
    col += (fbm(d * 3.0 + vec3(t * 0.002), 3)) * 0.04;
    col += vec3(stars(d, 0.015)) * smoothstep(0.1, 0.6, y) * 0.7;
    return col;
}

void main() {
    vec3 d = normalize(skyDir);
    float c = cos(NovaParams.z), s = sin(NovaParams.z);
    d = vec3(c * d.x - s * d.z, d.y, s * d.x + c * d.z);
    float t = mod(NovaTime, 100000.0);
    int mode = int(NovaParams.x + 0.5);
    vec3 col;
    if (mode == 0) col = nebula(d, t);
    else if (mode == 1) col = aurora(d, t);
    else if (mode == 2) col = cells(d, t);
    else if (mode == 3) col = starfield(d, t);
    else col = gradientSky(d, t);

    col *= NovaParams.y;
    col = vec3(1.0) - exp(-col * 1.4);                     // tono suave, sin quemar
    col *= clamp(1.0 + d.y * 5.0, 0.0, 1.0);               // bajo el horizonte se apaga
    col += (hash1(vec3(gl_FragCoord.xy, t)) - 0.5) / 255.0; // tramado: sin escalones de color
    fragColor = vec4(col, 1.0) * ColorModulator;
}
