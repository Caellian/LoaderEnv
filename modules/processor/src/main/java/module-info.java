open module loaderenv.processor {
    requires static java.compiler;
    requires static jdk.compiler;
    requires static loaderenv.lib;

    exports net.tinsvagelj.mc.loaderenv.javac;
}