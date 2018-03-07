
attribute vec2 aPosition;
attribute vec2 aTexCoords;

uniform mat4 uPositionMatrix;
uniform mat4 uTexCoordsMatrix;

varying vec2 vTexCoords;

void main() {

    gl_Position = uPositionMatrix * vec4(aPosition.x, aPosition.y, 0.0, 1.0);

    vTexCoords = (uTexCoordsMatrix * vec4(aTexCoords.x, aTexCoords.y, 0.0, 1.0)).xy;

}
