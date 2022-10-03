#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_outlineDistance; // Between 0 and 0.5, 0 = thick outline, 0.5 = no outline
uniform vec4 u_outlineColor;
uniform vec2 u_shadowOffset; // Between 0 and spread / textureSize
uniform float u_shadowSmoothing; // Between 0 and 0.5
uniform vec4 u_shadowColor;

varying vec4 v_color;
varying vec2 v_texCoord;

const float smoothing = 1.0/16.0;

void main() {
    float distance = texture2D(u_texture, v_texCoord).a;
    float outlineFactor = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);
    vec4 color = mix(u_outlineColor, v_color, outlineFactor);
    float alpha = smoothstep(u_outlineDistance - smoothing, u_outlineDistance + smoothing, distance);
    vec4 mainColor = vec4(color.rgb, color.a * alpha);

    float shadowDistance = texture2D(u_texture, v_texCoord - u_shadowOffset).a;
    float shadowAlpha = smoothstep(0.5 - u_shadowSmoothing, 0.5 + u_shadowSmoothing, shadowDistance);
    vec4 shadow = vec4(u_shadowColor.rgb, u_shadowColor.a * shadowAlpha);

    gl_FragColor = mix(shadow, mainColor, mainColor.a);
}
