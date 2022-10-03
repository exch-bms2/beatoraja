#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    vec4 c4 = texture2D(u_texture, v_texCoords);
    if(c4.r == 0.0 && c4.g == 0.0 && c4.b == 0.0) {
        gl_FragColor = v_color * vec4(c4.r, c4.g, c4.b, 0.0);
    } else {
        gl_FragColor = v_color * c4;
    }
}
