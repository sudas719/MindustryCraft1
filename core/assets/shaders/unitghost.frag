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

        // Water-ripple bands that propagate from top to bottom.
        float warp = sin(coords.x * 10.0 + u_time * 1.4) * 0.10 + sin(coords.x * 22.0 - u_time * 0.9) * 0.06;
        float band = fract((coords.y - u_time * 0.85 + warp) / stepSize);
        float line = step(band, lineW) + step(1.0 - lineW, band);
        line = min(line, 1.0);

        float wave = sin((coords.y - u_time * 0.85) * 18.0 + warp * 4.0);
        line *= 0.62 + 0.38 * (0.5 + 0.5 * wave);

        vec2 offset = vec2(sin((coords.y - u_time) * 45.0 + coords.x * 7.0) * 0.0026, 0.0);
        float warped = texture2D(u_texture, v_texCoords + offset).a;

        float inside = smoothstep(0.04, 0.18, warped);
        float ghost = clamp(edge * 0.90 + line * 0.72, 0.0, 1.0) * inside;
        gl_FragColor = vec4(u_color.rgb, u_color.a * ghost) * v_color;
        return;
    }

    if(u_mode < 2.5){
        // Distortion silhouette mode: shimmer inside the sprite alpha mask.
        float phase = (coords.y - u_time * 1.05) * 58.0 + sin(coords.x * 14.0 + u_time * 1.7) * 3.0;
        vec2 offset = vec2(sin(phase) * 0.0042, 0.0);

        float warped = texture2D(u_texture, v_texCoords + offset).a;
        float rim = smoothstep(0.02, 0.28, edge);
        float body = smoothstep(0.045, 0.24, warped);
        float ripple = 1.0 - smoothstep(0.0, 0.28, abs(sin(phase * 0.5)));
        float ghost = clamp(rim * 0.62 + ripple * 0.36 + body * 0.18, 0.0, 1.0);

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
