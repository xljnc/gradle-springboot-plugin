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

//    static final String EXTENSION_NAME = "qiyuSpringBoot";

    @Override
    void apply(Project project) {

        project.repositories {
            mavenLocal()
            maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
            mavenCentral()
        }

        project.ext {
            if (!hasExtraProperty(project, "sourceCompatibility"))
                sourceCompatibility = 11
            if (!hasExtraProperty(project, "jarArchiveClassifier") && !hasExtraProperty(project, "archiveClassifier"))
                jarArchiveClassifier = ''
            if (!hasExtraProperty(project, "sourceJarArchiveClassifier"))
                sourceJarArchiveClassifier = 'sources'
            if (!hasExtraProperty(project, "docJarArchiveClassifier"))
                docJarArchiveClassifier = 'javadoc'
        }

        project.apply plugin: 'io.spring.dependency-management'

        project.apply plugin: 'maven-publish'

        def publicationsClosure = {
            maven(MavenPublication) {
                groupId project.group
                version project.version
                versionMapping {
                    usage('java-api') {
                        fromResolutionOf('runtimeClasspath')
                    }
                    usage('java-runtime') {
                        fromResolutionResult()
                    }
                }
            }
        }
        publicationsClosure.setDelegate(project)
        project.publishing.publications(publicationsClosure)

        project.subprojects.each {
            it.group project.group
            it.version project.version

            it.extensions.extraProperties.set("sourceCompatibility", project.ext.sourceCompatibility)

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

            Project curr = it

            def sourceJarTask = it.task('sourceJar', type: Jar, group: 'build') {
                archiveClassifier = project.ext.sourceJarArchiveClassifier
                JavaPluginExtension javaPluginExtension = curr.extensions.getByType(JavaPluginExtension)
                SourceSetContainer sourceSets = javaPluginExtension.sourceSets
                from sourceSets.main.allSource
            }

            def jarTask = it.tasks.getByName('jar')
            jarTask.setProperty("archiveClassifier", project.ext.jarArchiveClassifier)

            def bootJarTask = it.tasks.getByName('bootJar')
            bootJarTask.setProperty("enabled", false)

            def subProjectPublicationsClosure = {
                maven(MavenPublication) {
                    groupId curr.group
                    version curr.version
                    from curr.components.getByName("java")
                    artifact bootJarTask
                    artifact sourceJarTask
                    versionMapping {
                        usage('java-api') {
                            fromResolutionOf('runtimeClasspath')
                        }
                        usage('java-runtime') {
                            fromResolutionResult()
                        }
                    }
                }
            }
            subProjectPublicationsClosure.setDelegate(curr)
            curr.publishing.publications(subProjectPublicationsClosure)
        }


    }

    private boolean hasExtraProperty(Project project, String name) {
        return project.extensions.extraProperties.has(name);
    }
}
