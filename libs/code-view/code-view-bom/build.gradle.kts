plugins {
    `java-platform`
    `maven-publish`
}

group = "io.github.dexclub"
version = providers.gradleProperty("code-view.version").orElse("0.1.0").get()

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":code-view-core"))
        api(project(":code-view-language"))
        api(project(":code-view-runtime"))
        api(project(":code-view-compose"))
        api(project(":code-view-tree-sitter"))
        api(project(":code-view-tree-sitter-java"))
        api(project(":code-view-tree-sitter-kotlin"))
        api(project(":code-view-tree-sitter-smali"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            artifactId = "code-view-bom"
            from(components["javaPlatform"])
            pom {
                name.set("code-view BOM")
                description.set("Bill of Materials for code-view modules")
                url.set("https://github.com/dexclub/code-view")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
            }
        }
    }
}
