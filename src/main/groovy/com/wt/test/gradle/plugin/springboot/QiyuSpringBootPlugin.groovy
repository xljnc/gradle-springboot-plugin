package com.wt.test.gradle.plugin.springboot

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 *
 * @author 一贫
 * @date 2021/12/10
 */
 class QiyuSpringBootPlugin implements Plugin<Project>{

     static final String EXTENSION_NAME = "qiyuSpringBoot";

     @Override
     void apply(Project project) {
         project.extensions.create(EXTENSION_NAME, SpringBootPluginExtension);
         project.repositories{
             mavenLocal()
             maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
             mavenCentral()
         }
         project.buildscript {
             ext {
                 sourceCompatibility = 11
                 archiveClassifier = ''
                 springbootVersion = '2.6.1'
                 dependencyManagementPluginVersion = '1.0.11.RELEASE'
             }
             repositories {
                 mavenLocal()
                 maven { name "Alibaba"; url "https://maven.aliyun.com/repository/public" }
                 mavenCentral()
             }
             dependencies {
                 classpath "org.springframework.boot:spring-boot-gradle-plugin:${springbootVersion}"
                 classpath 'io.spring.gradle:dependency-management-plugin:${dependencyManagementPluginVersion}'
             }
         }
         project.apply{
             it.plugin("java")
             it.plugin("java-library")
             it.plugin("io.spring.dependency-management")
             it.plugin("maven-publish")
         }

         project.subprojects {
             group group
             version version
             sourceCompatibility = ext.sourceCompatibility

             apply plugin: 'java'
             apply plugin: 'io.spring.dependency-management'
             apply plugin: 'org.springframework.boot'
             apply plugin: 'java-library'
             apply plugin: 'maven-publish'

             bootJar {
                 enabled = false
             }
         }

         project.allprojects {
             jar {
                 archiveClassifier = ext.archiveClassifier
             }

             publishing {
                 publications {
                     mavenLocal(MavenPublication) {
                         from components.java
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
             }
         }
     }
 }
