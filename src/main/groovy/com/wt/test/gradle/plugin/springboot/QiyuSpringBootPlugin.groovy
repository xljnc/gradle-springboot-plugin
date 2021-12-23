package com.wt.test.gradle.plugin.springboot

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.internal.bridge.InternalComponents
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.repositories.resolver.MavenResolver
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar

/**
 *
 *
 * @author 一贫
 * @date 2021/12/10
 */
class QiyuSpringBootPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "qiyuSpringBoot";

    static final String DEPENDENCY_MANAGEMENT_EXTENSION_NAME = "dependencyManagement";

    @Override
    void apply(Project project) {
        def qiyuSpringBoot = project.extensions.create(EXTENSION_NAME, SpringBootPluginExtension);

        InternalComponents internalComponents = new InternalComponents(project);

        final DependencyManagementExtension dependencyManagementExtension =
                internalComponents.getDependencyManagementExtension();

        project.extensions.add(DEPENDENCY_MANAGEMENT_EXTENSION_NAME, dependencyManagementExtension);

        project.repositories {
            mavenLocal()
            maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
            mavenCentral()
        }

        project.ext {
            sourceCompatibility = qiyuSpringBoot.sourceCompatibility
        }

        project.subprojects.each {
            it.group project.getGroup()
            it.version project.getVersion()

            it.extensions.extraProperties.set("sourceCompatibility", qiyuSpringBoot.sourceCompatibility == null ? 11 : qiyuSpringBoot.sourceCompatibility)

            it.repositories {
                mavenLocal()
                maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
                mavenCentral()
            }

            it.apply plugin: 'java'
            it.apply plugin: 'java-library'
            it.apply plugin: 'org.springframework.boot'
            it.apply plugin: 'maven-publish'
//            it.apply plugin: 'io.spring.dependency-management'

            configDependencyManagement(it,dependencyManagementExtension);

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

    private void configDependencyManagement(Project project,DependencyManagementExtension dependencyManagementExtension) {
        InternalComponents internalComponents = new InternalComponents(project);

//        project.extensions.add(DEPENDENCY_MANAGEMENT_EXTENSION_NAME, dependencyManagementExtension);

        internalComponents.createDependencyManagementReportTask("dependencyManagement");

        project.getConfigurations().all(internalComponents.getImplicitDependencyManagementCollector());
        project.getConfigurations().all(internalComponents.getDependencyManagementApplier());

        configurePomCustomization(project, dependencyManagementExtension);

    }

    private void configurePomCustomization(final Project project, DependencyManagementExtension dependencyManagementExtension) {
        final PomDependencyManagementConfigurer pomConfigurer = dependencyManagementExtension.getPomConfigurer();
        project.getTasks().withType(Upload.class, new Action<Upload>() {

            @Override
            public void execute(final Upload upload) {
                upload.doFirst(new Action<Task>() {

                    @Override
                    public void execute(Task task) {
                        upload.getRepositories().withType(MavenResolver.class, new Action<MavenResolver>() {

                            @Override
                            public void execute(MavenResolver mavenResolver) {
                                mavenResolver.getPom().withXml(pomConfigurer);
                            }

                        });
                    }

                });

            }

        });
        project.getPlugins().withType(MavenPublishPlugin.class, new Action<MavenPublishPlugin>() {

            @Override
            public void execute(MavenPublishPlugin mavenPublishPlugin) {
                configurePublishingExtension(project, pomConfigurer);
            }

        });
    }

    private void configurePublishingExtension(Project project, final PomDependencyManagementConfigurer extension) {
        project.getExtensions().configure(PublishingExtension.class, new Action<PublishingExtension>() {

            @Override
            public void execute(PublishingExtension publishingExtension) {
                configurePublications(publishingExtension, extension);
            }

        });
    }

    private void configurePublications(PublishingExtension publishingExtension,
                                       final PomDependencyManagementConfigurer extension) {
        publishingExtension.getPublications().withType(MavenPublication.class, new Action<MavenPublication>() {

            @Override
            public void execute(MavenPublication mavenPublication) {
                mavenPublication.getPom().withXml(extension);
            }

        });
    }
}
