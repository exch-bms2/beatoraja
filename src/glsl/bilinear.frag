#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec4 dst_color;
uniform int filter_type;

void main() {
  if(filter_type == 1) {
    vec2 texSize = textureSize(u_texture,0);
	float texelSizeX = 1.0 / texSize.x;
	float texelSizeY = 1.0 / texSize.y;

    vec4 p0q0 = texture2D(u_texture, v_texCoords);
    float p0q0a = p0q0.a;
    vec4 p1q0 = texture2D(u_texture, v_texCoords + vec2(texelSizeX, 0));

    vec4 p0q1 = texture2D(u_texture, v_texCoords + vec2(0, texelSizeY));
    vec4 p1q1 = texture2D(u_texture, v_texCoords + vec2(texelSizeX , texelSizeY));

    float a = fract(v_texCoords.x * texSize.x);

    vec4 pInterp_q0 = mix( p0q0 * p0q0.a, p1q0 * p1q0.a, a);
    vec4 pInterp_q1 = mix( p0q1 * p0q1.a, p1q1 * p1q1.a, a);

    float b = fract(v_texCoords.y * texSize.y);

    vec4 result = mix( pInterp_q0, pInterp_q1, b );
    result.a = p0q0a;
	gl_FragColor = dst_color * result;
  } else {
	gl_FragColor = dst_color * texture2D(u_texture, v_texCoords);
  }
}
