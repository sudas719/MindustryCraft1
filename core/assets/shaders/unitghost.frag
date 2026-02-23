uniform sampler2D u_texture;

uniform vec4 u_color;
uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform vec2 u_texsize;
uniform float u_mode;
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
    float stepSize = max(u_lineStep, 0.001);
    float lineW = clamp(u_lineWidth, 0.001, 0.45);
    // move scanlines from front(y=0) to back(y=1)
    float band = fract((coords.y - u_time * 0.9) / stepSize);
    float line = step(band, lineW) + step(1.0 - lineW, band);
    line = min(line, 1.0);

    float inside = smoothstep(0.04, 0.18, alpha);
    float ghost = clamp(edge * 0.95 + line * 0.55, 0.0, 1.0) * inside;
    gl_FragColor = vec4(u_color.rgb, u_color.a * ghost) * v_color;
}
