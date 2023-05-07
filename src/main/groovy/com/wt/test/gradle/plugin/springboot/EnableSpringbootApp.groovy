package com.wt.test.gradle.plugin.springboot

import org.gradle.api.Project

/**
 * 启用SpringBoot 打 boot jar任务
 *
 * @author qiyu
 * @since 2023/5/7
 */
class EnableSpringbootApp {

    //启用SpringBoot 打 boot jar任务
    def enableBootApp(Project project) {
        def jarTask = project.tasks.getByName('jar')
        jarTask.enabled = false

        def bootJarTask = project.tasks.getByName('bootJar');
        bootJarTask.enabled = true

        project.tasks.named('generateMetadataFileForMavenPublication') {
            dependsOn(bootJarTask)
        }
    }
}
