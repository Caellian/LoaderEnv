import com.vanniktech.maven.publish.SonatypeHost
import net.tinsvagelj.mc.loaderenv.build.*

plugins {
    id("com.vanniktech.maven.publish")
    signing
}

allprojects {
    apply(plugin = "idea")
    apply(plugin = "eclipse")

    group = "net.tinsvagelj.mc"
}

configure(listOf(
    project(":lib"),
    project(":processor"),
    project(":gradle"),
)) {
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "signing")

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        repositories {
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/caellian/loaderenv")
                credentials(PasswordCredentials::class)
            }
        }
        signAllPublications()
    }

    val pomExt = extensions.create("packageInfo", PackageInfo::class.java)
    afterEvaluate {
        pomExt.configure(project)
    }
}

configure(listOf(project(":processor"))) {
    extensions.create("moduleOptions", ModuleOptions::class.java, project)
}
