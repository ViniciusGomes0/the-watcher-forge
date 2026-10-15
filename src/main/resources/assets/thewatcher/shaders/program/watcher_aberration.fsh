#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Intensity;   // 0..1, escalado pela proximidade (setado por código)
uniform vec2 WatcherPos;   // posição do Watcher na tela, 0..1 (setado por código)

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;

    // Distância radial até o ponto do Watcher na tela (corrigindo aspecto).
    vec2 d = uv - WatcherPos;
    d.x *= OutSize.x / OutSize.y;
    float dist = length(d);

    // Halo: forte perto do Watcher, some ao longe (raio ~0.35).
    float halo = clamp(1.0 - dist / 0.35, 0.0, 1.0);
    float amount = Intensity * halo;

    if (amount <= 0.001) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    // Aberração cromática: canais R/B deslocados radialmente.
    vec2 dir = normalize(d + vec2(0.00001));
    vec2 offset = dir * amount * 0.012;
    float r = texture(DiffuseSampler, uv + offset).r;
    float g = texture(DiffuseSampler, uv).g;
    float b = texture(DiffuseSampler, uv - offset).b;
    vec3 color = vec3(r, g, b);

    // Desfoque leve (cruz de 4 amostras), misturado pela intensidade.
    vec2 px = (amount * 2.0) / InSize;
    vec3 blur = color;
    blur += texture(DiffuseSampler, uv + vec2(px.x, 0.0)).rgb;
    blur += texture(DiffuseSampler, uv - vec2(px.x, 0.0)).rgb;
    blur += texture(DiffuseSampler, uv + vec2(0.0, px.y)).rgb;
    blur += texture(DiffuseSampler, uv - vec2(0.0, px.y)).rgb;
    blur /= 5.0;

    vec3 finalColor = mix(color, blur, clamp(amount, 0.0, 1.0));
    fragColor = vec4(finalColor, 1.0);
}
