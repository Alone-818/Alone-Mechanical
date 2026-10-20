#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec4 water = texture(Sampler0, texCoord0);
    float value = 0.68 + 0.32 * dot(water.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (water.a < 0.01) discard;
    vec4 color = vec4(vec3(value), 1.0) * vertexColor * ColorModulator;
    if (color.a < 0.01) discard;
    fragColor = color;
}
