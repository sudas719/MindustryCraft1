#define MAX_SHOCKWAVES 64
#define MAX_LENSES 32
#define WAVE_RADIUS 5.0
#define DIFF_SCL 1.5
#define WAVE_POW 0.8

varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform vec2 u_campos;
uniform vec4 u_shockwaves[MAX_SHOCKWAVES];
uniform int u_shockwave_count;
uniform vec4 u_lenses[MAX_LENSES];
uniform vec4 u_lens_meta[MAX_LENSES];
uniform int u_lens_count;

vec2 lensDisplacement(vec2 worldCoords, vec4 lens, vec4 meta){
    vec2 center = lens.xy;
    vec2 local = worldCoords - center;
    float rx = max(lens.z, 0.0001);
    float ry = max(lens.w, 0.0001);

    float angle = meta.x;
    float t = clamp(meta.y, 0.0, 1.0);
    float typeId = meta.z;
    float strength = max(meta.w, 0.0);

    float d;
    if(typeId < 0.5){
        float c = cos(-angle), s = sin(-angle);
        vec2 rotated = vec2(local.x * c - local.y * s, local.x * s + local.y * c);
        d = length(vec2(rotated.x / rx, rotated.y / ry));
    }else{
        d = length(local) / rx;
    }

    float len = length(local);
    vec2 dir = len > 0.0001 ? local / len : vec2(0.0, 0.0);

    //instant bulge, then shrink and fade
    float expand = clamp(t / 0.10, 0.0, 1.0);
    float recover = clamp((t - 0.52) / 0.48, 0.0, 1.0);
    float bodyPhase = expand * (1.0 - recover);

    float inside = clamp(1.0 - d, 0.0, 1.0);
    float core = inside * inside * (1.0 + 0.85 * inside);
    float bulge;
    if(typeId < 0.5){
        //ellipse lens: classic center bulge
        bulge = strength * bodyPhase * core * 14.0;
    }else{
        //sphere lens: push from around half-radius, not from the exact center
        float band = exp(-pow((d - 0.55) / 0.22, 2.0));
        float innerFade = smoothstep(0.18, 0.38, d);
        float outerFade = 1.0 - smoothstep(0.92, 1.08, d);
        float shell = band * innerFade * outerFade;
        bulge = -strength * bodyPhase * shell * 16.0;
    }

    //ending static edge wave: fades only, no movement
    float edgePhase = clamp((t - 0.60) / 0.40, 0.0, 1.0);
    float edgeBand = exp(-pow((d - 1.0) / 0.085, 2.0));
    float edge = strength * edgeBand * edgePhase * (1.0 - edgePhase) * 8.0;

    return dir * (bulge + edge) / u_resolution;
}

void main(){
    vec2 worldCoords = v_texCoords * u_resolution + u_campos;
    vec2 uv = v_texCoords;
    vec2 displacement = vec2(0.0, 0.0);

    for(int i = 0; i < MAX_SHOCKWAVES; i ++){
        vec4 wave = u_shockwaves[i];
        float radius = wave.z;
        float dst = distance(worldCoords, wave.xy);
        float strength = wave.w * (1.0 - abs(dst - radius) / WAVE_RADIUS);

        if(abs(dst - radius) <= WAVE_RADIUS){
            float diff = (dst - radius);

            float pdiff = 1.0 - pow(abs(diff * DIFF_SCL), WAVE_POW);
            float diffTime = diff  * pdiff;
            vec2 relative = normalize(worldCoords - wave.xy);

            displacement += (relative * diffTime * strength) / u_resolution;
        }

        if(i >= u_shockwave_count - 1){
            break;
        }
    }

    for(int i = 0; i < MAX_LENSES; i ++){
        if(i >= u_lens_count){
            break;
        }

        displacement += lensDisplacement(worldCoords, u_lenses[i], u_lens_meta[i]);
    }

    vec4 c = texture2D(u_texture, uv + displacement);
    gl_FragColor = c;
}
