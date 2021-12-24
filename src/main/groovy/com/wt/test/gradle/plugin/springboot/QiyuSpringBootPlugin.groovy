package com.wt.test.gradle.plugin.springboot


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar

/**
 *
 *
 * @author 一贫
 * @date 2021/12/10
 */
class QiyuSpringBootPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "qiyuSpringBoot";

    @Override
    void apply(Project project) {

        def qiyuSpringBoot = project.extensions.create(EXTENSION_NAME, SpringBootPluginExtension);

        project.repositories {
            mavenLocal()
            maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
            mavenCentral()
        }

        project.ext {
            sourceCompatibility = qiyuSpringBoot.sourceCompatibility
        }

        project.subprojects.each {
            it.group project.group
            it.version project.version

            it.extensions.extraProperties.set("sourceCompatibility", qiyuSpringBoot.sourceCompatibility == null ? 11 : qiyuSpringBoot.sourceCompatibility)

            it.repositories {
                mavenLocal()
                maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
                mavenCentral()
            }

            it.buildscript {
                dependencies {
                    classpath "org.springframework.boot:spring-boot-gradle-plugin:2.6.2"
                    classpath "io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE"
                }
            }

            it.apply plugin: 'java'
            it.apply plugin: 'java-library'
            it.apply plugin: 'org.springframework.boot'
            it.apply plugin: 'maven-publish'
            it.apply plugin: 'io.spring.dependency-management'

            it.getTasks().each { task ->
                if (task.name == 'jar') {
                    task.setProperty("archiveClassifier", qiyuSpringBoot.jarArchiveClassifier)
                } else if (task.name == 'bootJar') {
                    task.setProperty("enabled", false)
                }
            }

            Project curr = it

            def sourceJarTask = it.task('sourceJar', type: Jar, group: 'build', dependsOn: ['clean', 'classes']) {
                archiveClassifier = qiyuSpringBoot.sourceJarArchiveClassifier
                JavaPluginExtension javaPluginExtension = curr.extensions.getByType(JavaPluginExtension)
                SourceSetContainer sourceSets = javaPluginExtension.sourceSets
                from sourceSets.main.allSource
            }

            def subProjectPublicationsClosure = {
                maven(MavenPublication) {
                    groupId project.group
                    version project.version
//                    from components.java
                    artifact sourceJarTask
                }
            }
            subProjectPublicationsClosure.setDelegate(it)
            it.publishing.publications(subProjectPublicationsClosure)
        }

        project.apply plugin: 'maven-publish'

        def publicationsClosure = {
            maven(MavenPublication) {
                groupId project.group
                version project.version
//                    from components.java
            }
        }
        publicationsClosure.setDelegate(project)
        project.publishing.publications(publicationsClosure)
    }
}
