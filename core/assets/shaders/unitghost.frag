uniform sampler2D u_texture;

uniform vec4 u_color;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform vec2 u_texsize;
uniform float u_mode;
uniform float u_progress;
uniform float u_lineStep;
uniform float u_lineWidth;
uniform float u_time;

varying vec4 v_color;
varying vec2 v_texCoords;

void main(){
    float alpha = texture2D(u_texture, v_texCoords).a;
    if(alpha <= 0.001){
        gl_FragColor = vec4(0.0);
        return;
    }

    if(u_mode < 0.5){
        gl_FragColor = vec4(u_color.rgb, u_color.a * alpha) * v_color;
        return;
    }

    vec2 px = vec2(1.0 / u_texsize.x, 1.0 / u_texsize.y);
    float aL = texture2D(u_texture, v_texCoords - vec2(px.x, 0.0)).a;
    float aR = texture2D(u_texture, v_texCoords + vec2(px.x, 0.0)).a;
    float aD = texture2D(u_texture, v_texCoords - vec2(0.0, px.y)).a;
    float aU = texture2D(u_texture, v_texCoords + vec2(0.0, px.y)).a;

    float edge = max(max(abs(alpha - aL), abs(alpha - aR)), max(abs(alpha - aD), abs(alpha - aU)));
    edge = smoothstep(0.03, 0.20, edge);

    vec2 coords = (v_texCoords - u_uv) / (u_uv2 - u_uv);

    if(u_mode < 1.5){
        float stepSize = max(u_lineStep, 0.001);
        float lineW = clamp(u_lineWidth, 0.001, 0.45);
        // move scanlines from front(y=0) to back(y=1)
        float band = fract((coords.y - u_time * 0.9) / stepSize);
        float line = step(band, lineW) + step(1.0 - lineW, band);
        line = min(line, 1.0);

        float inside = smoothstep(0.04, 0.18, alpha);
        float ghost = clamp(edge * 0.95 + line * 0.55, 0.0, 1.0) * inside;
        gl_FragColor = vec4(u_color.rgb, u_color.a * ghost) * v_color;
        return;
    }

    if(u_mode < 2.5){
        // Distortion silhouette mode: shimmer inside the sprite alpha mask.
        float waveA = sin(coords.y * 70.0 + u_time * 7.0);
        float waveB = sin(coords.x * 95.0 - u_time * 8.5);
        vec2 offset = vec2((waveA + waveB) * 0.003, (waveB - waveA) * 0.002);

        float warped = texture2D(u_texture, v_texCoords + offset).a;
        float body = smoothstep(0.035, 0.22, warped);
        float rim = smoothstep(0.02, 0.28, edge);
        float shimmer = 0.5 + 0.5 * sin((coords.x + coords.y) * 46.0 + u_time * 11.0);
        float ghost = clamp(body * (0.34 + shimmer * 0.30) + rim * 0.44, 0.0, 1.0);

        gl_FragColor = vec4(u_color.rgb, u_color.a * ghost) * v_color;
        return;
    }

    // Cloak-enter mode: irregular burning-paper dissolve into translucency.
    vec4 tex = texture2D(u_texture, v_texCoords);
    vec2 cell = floor(coords * vec2(26.0, 30.0) + vec2(coords.y * 5.0, coords.x * 3.0));
    float n0 = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453123);
    float n1 = fract(sin(dot(cell + vec2(9.2, 17.7), vec2(269.5, 183.3))) * 24634.6345);
    float flow = sin((coords.x * 17.0 - coords.y * 13.0) + u_time * 3.1) * 0.08;
    float noise = clamp(n0 * 0.65 + n1 * 0.35 + flow, 0.0, 1.0);
    float p = clamp(u_progress, 0.0, 1.0);

    // remain=1 -> still visible body; remain=0 -> dissolved into cloak state.
    float remain = smoothstep(p - 0.055, p + 0.055, noise);
    float burnBand = 1.0 - smoothstep(0.0, 0.09, abs(noise - p));
    float ghostInside = smoothstep(0.04, 0.18, alpha);

    vec3 ash = mix(vec3(0.16, 0.17, 0.19), vec3(0.95, 0.68, 0.46), burnBand);
    vec3 rgb = mix(u_color.rgb, tex.rgb, remain);
    rgb = mix(rgb, ash, burnBand * (1.0 - remain) * 0.88);

    float finalAlpha = alpha * (remain + (1.0 - remain) * 0.38 * ghostInside);
    gl_FragColor = vec4(rgb, u_color.a * finalAlpha) * v_color;
}
