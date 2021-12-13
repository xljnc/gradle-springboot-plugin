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

//        project.buildscript.dependencies.add("classpath", "org.springframework.boot:spring-boot-gradle-plugin:2.6.1")
//        project.buildscript.dependencies.add("classpath", "io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")

        project.repositories {
            mavenLocal()
            maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
            mavenCentral()
        }

        project.allprojects.each {
            it.group project.getGroup()
            it.version project.getVersion()

            it.ext {
                sourceCompatibility = qiyuSpringBoot.sourceCompatibility
            }

            it.repositories {
                mavenLocal()
                maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
                mavenCentral()
            }

            it.apply plugin: 'java'
            it.apply plugin: 'java-library'
            it.apply plugin: 'maven-publish'
            it.apply plugin: 'io.spring.dependency-management'

            it.getTasks().each { task ->
                if (task.name == 'jar')
                    task.setProperty("archiveClassifier", qiyuSpringBoot.jarArchiveClassifier)
            }

            Project curr = it

            def sourceJarTask = it.task('sourceJar', type: Jar, group: 'build' ,dependsOn: 'classes') {
                archiveClassifier = qiyuSpringBoot.sourceJarArchiveClassifier
                JavaPluginExtension javaPluginExtension = curr.extensions.getByType(JavaPluginExtension)
                SourceSetContainer sourceSets = javaPluginExtension.sourceSets
                from sourceSets.main.allSource
            }

            def publicationsClosure = {
                maven(MavenPublication) {
                    groupId project.group
                    version project.version
//                    from components.java
                    artifact sourceJarTask
                }
            }
            publicationsClosure.setDelegate(project)
            it.publishing.publications(publicationsClosure)
        }

        project.subprojects.each {
            it.group project.getGroup()
            it.version project.getVersion()

            it.apply plugin: 'org.springframework.boot'
            it.getTasks().each {
                if (it.name == 'bootJar')
                    it.setProperty("enabled", false)
            }
        }
    }
}
