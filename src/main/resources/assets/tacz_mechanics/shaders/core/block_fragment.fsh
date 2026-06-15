#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 lightMapColor;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 particleShape = texture(Sampler3, texCoord1);
    if (particleShape.a < 0.1) {
        discard;
    }
    vec4 blockColor = texture(Sampler0, texCoord0);
    vec4 color = blockColor * vertexColor * ColorModulator * lightMapColor;
    if (color.a < 0.05) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
