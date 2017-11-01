#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform int filter_type;
uniform int image_type;

void main() {
    vec2 texSize = textureSize(u_texture,0);

    float center_a = texture2D(u_texture, v_texCoords).a;
    if(center_a > 0.0) {
      float texelSizeX = 0.5 / texSize.x;
      float texelSizeY = 0.5 / texSize.y;
      vec4 p0q0 = texture2D(u_texture, v_texCoords + vec2(-texelSizeX, -texelSizeY));
      vec4 p1q0 = texture2D(u_texture, v_texCoords + vec2(texelSizeX, -texelSizeY));

      vec4 p0q1 = texture2D(u_texture, v_texCoords + vec2(-texelSizeX, texelSizeY));
      vec4 p1q1 = texture2D(u_texture, v_texCoords + vec2(texelSizeX , texelSizeY));

      float a = fract(v_texCoords.x * texSize.x + 0.5);

      vec4 pInterp_q0 = mix( p0q0 * p0q0.a, p1q0 * p1q0.a, a);
      vec4 pInterp_q1 = mix( p0q1 * p0q1.a, p1q1 * p1q1.a, a);

      float b = fract(v_texCoords.y * texSize.y + 0.5);

      vec4 result = mix( pInterp_q0, pInterp_q1, b ) / center_a;
      result.a = center_a;
	  gl_FragColor = v_color * result;
    } else {
	  gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
