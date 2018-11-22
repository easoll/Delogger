package com.android.build.gradle.tasks.factory

import com.android.build.gradle.api.BaseVariant
import com.android.utils.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.collections.DefaultConfigurableFileTree
import org.gradle.api.tasks.SourceTask

class DeloggerPluginImp implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.add("delogger", DeloggerExtension)

        project.afterEvaluate({
            DeloggerExtension cleanExtension = project.delogger

            if(cleanExtension.prefixList == null ||
                    cleanExtension.prefixList.isEmpty()){  //set a default value
                cleanExtension.prefixList = "Logger.;Log."
            }

            def prefixList = cleanExtension.prefixList.split(";")

            project.android.applicationVariants.all { BaseVariant variant ->
                String variantName = variant.name.capitalize()

                Task compileJavaTask = project.tasks.getByName("compile${variantName}JavaWithJavac")

                //hook javac compile
                compileJavaTask.doFirst {
                    if(compileJavaTask instanceof AndroidJavaCompile){
                        def androidJavaCompile = (AndroidJavaCompile) compileJavaTask
                        println androidJavaCompile.getProcessorListFile()

                        def sourceField = SourceTask.class.getDeclaredField("source")
                        sourceField.setAccessible(true)
                        def source = (List<DefaultConfigurableFileTree>)sourceField.get(androidJavaCompile)
                        def iterator = source.iterator()
                        while(iterator.hasNext()){
                            def fileTree = iterator.next()
                            if(fileTree instanceof DefaultConfigurableFileTree) {
                                if (fileTree.dir.absolutePath.endsWith("src/main/java")) {  //replace source java dir
                                    def cleanDirPath = project.buildDir.absolutePath + File.separator + "delogger"
                                    def cleanDir = new File(cleanDirPath)
                                    cleanDir.mkdirs()
                                    FileUtils.copyDirectory(fileTree.dir, cleanDir)

                                    fileTree.from(cleanDir)

                                    doClean(cleanDir, prefixList)
                                }
                            }
                        }
                    }
                }
            }

        })
    }

    private void doClean(File source, String[] prefixList){
        if(source.isFile()){
            boolean isJavaFile = source.name.endsWith(".java")
            boolean isKotlinFile = source.name.endsWith(".kt")

            def outputFile = new File(source.parentFile, "tmp_${source.name}\$")

            boolean statementEnd = true
            for (singleLine in source.readLines()) {
                def trimmed = singleLine.trim()

                if (!startWith(trimmed, prefixList)) {
                    if(statementEnd) {
                        outputFile.append(singleLine)
                    }else{
                        if(isJavaFile) {
                            statementEnd = trimmed.endsWith(";")
                        }else if(isKotlinFile){
                            statementEnd = trimmed.endsWith(")")
                        }else{
                            throw UnsupportedOperationException("not support file type")
                        }
                    }
                }else{
                    if(isJavaFile) {
                        statementEnd = trimmed.endsWith(";")
                    }else if(isKotlinFile){
                        statementEnd = trimmed.endsWith(")")
                    }else{
                        throw UnsupportedOperationException("not support file type")
                    }
                }

                outputFile.append("\n")  //keep the line number correct
            }

            outputFile.renameTo(source)
        }else{
            def children = source.listFiles()
            for(file in children){
                doClean(file, prefixList)
            }
        }
    }

    private static boolean startWith(String trimmedLine, String[] prefixList){
        for(prefix in prefixList){
            if(trimmedLine.startsWith(prefix)){
                return true
            }
        }

        return false
    }
}