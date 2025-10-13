package net.irisshaders.iris.gl.buffer;

public record ShaderStorageInfo(long size, boolean relative, float scaleX, float scaleY) {
    public String name() {
        return "";
    }
}