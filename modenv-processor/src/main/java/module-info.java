open module modenv.processor {
    requires static java.compiler;
    requires static jdk.compiler;
    requires static modenv.lib;

    exports net.tinsvagelj.mc.modenv.javac;
}