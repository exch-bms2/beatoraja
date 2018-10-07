#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_outlineDistance; // Between 0 and 0.5, 0 = thick outline, 0.5 = no outline
uniform vec4 u_outlineColor;

varying vec4 v_color;
varying vec2 v_texCoord;

const float smoothing = 1.0/16.0;

void main() {
    float distance = texture2D(u_texture, v_texCoord).a;
    float outlineFactor = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);
    vec4 color = mix(u_outlineColor, v_color, outlineFactor);
    float alpha = smoothstep(u_outlineDistance - smoothing, u_outlineDistance + smoothing, distance);
    gl_FragColor = vec4(color.rgb, color.a * alpha);
}
