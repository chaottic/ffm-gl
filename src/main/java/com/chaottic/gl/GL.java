package com.chaottic.gl;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public final class GL {

    private static final MethodHandle GL_CREATE_VERTEX_ARRAYS;

    static {
        try (MemorySession session = MemorySession.openConfined()) {
            var lookup = SymbolLookup.libraryLookup("opengl32.dll", session);

            var allocated = SegmentAllocator.newNativeArena(session);

            var linker = Linker.nativeLinker();

            var wglGetProcAddress = linker.downcallHandle(lookup.lookup("wglGetProcAddress").orElseThrow(), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

            GL_CREATE_VERTEX_ARRAYS = getMethodHandle("glCreateVertexArrays", allocated, wglGetProcAddress, linker, FunctionDescriptor.ofVoid(JAVA_INT, JAVA_LONG));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private GL() {}

    public static void glCreateVertexArrays(int i, long l) throws Throwable {
        GL_CREATE_VERTEX_ARRAYS.invoke(i, l);
    }

    private static MethodHandle getMethodHandle(String func, SegmentAllocator allocator, MethodHandle getter, Linker linker, FunctionDescriptor descriptor) throws Throwable {
        var allocated = allocator.allocateUtf8String(func);

        var address = MemoryAddress.ofLong((long) getter.invoke(allocated));

        return linker.downcallHandle(address, descriptor);
    }

    @OnlyOn(os = OS.WINDOWS)
    private static MemoryAddress getMemoryAddress(String func, SegmentAllocator allocator, MethodHandle wglGetProcAddress, MethodHandle getProcAddress, long module) throws Throwable {
        var allocated = allocator.allocateUtf8String(func);

        var l = (long) wglGetProcAddress.invoke(allocated);
        if (l == 0) {
            l = (long) getProcAddress.invoke(module, allocated);
        }

        return MemoryAddress.ofLong(l);
    }
}
