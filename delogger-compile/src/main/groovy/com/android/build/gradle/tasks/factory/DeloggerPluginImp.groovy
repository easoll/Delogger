package com.android.build.gradle.tasks.factory

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.AndroidJavaCompile
import com.android.utils.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.collections.DefaultConfigurableFileTree
import org.gradle.api.tasks.SourceTask

class DeloggerPluginImp implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.add("delogger", DeloggerExtension)

        project.afterEvaluate({
            //find the target task name
//            for(task in project.tasks){
//                for(inputFile in task.inputs.files){
//                    if(inputFile.absolutePath.contains("src/main/java")){
//                        println "================== taskName: " + task.name
//                    }
//                }
//            }


            DeloggerExtension cleanExtension = project.delogger

            if(cleanExtension.prefixList == null ||
                    cleanExtension.prefixList.isEmpty()){  //set a default value
                cleanExtension.prefixList = "Logger.;Log."
            }

            def prefixList = cleanExtension.prefixList.split(";")
            def cleanDirPath = project.buildDir.absolutePath + File.separator + "delogger"
            def hasDoClean = false

            project.android.applicationVariants.all { BaseVariant variant ->

                String variantName = variant.name.capitalize()

                //hook kotlin compile
                Task compileKotlinTask = project.tasks.getByName("compile${variantName}Kotlin")
                println "============================ get compileKotlinTask"
                compileKotlinTask.doFirst {
//                    def clazz_KotlinCompile = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")  // can not find class by Class.forName
                    def clazz_KotlinCompile = compileKotlinTask.class.superclass
                    def field_sourceRootsContainer = clazz_KotlinCompile.getDeclaredField("sourceRootsContainer")
                    field_sourceRootsContainer.setAccessible(true)
                    def sourceRootsContainer = field_sourceRootsContainer.get(compileKotlinTask)
//                    def clazz_FilteringSourceRootsContainer = Class.forName("org.jetbrains.kotlin.gradle.tasks.FilteringSourceRootsContainer")
                    def clazz_FilteringSourceRootsContainer = sourceRootsContainer.class
                    def field_mutableSourceRoots = clazz_FilteringSourceRootsContainer.getDeclaredField("mutableSourceRoots")
                    field_mutableSourceRoots.setAccessible(true)
                    def mutableSourceRoots = (ArrayList<File>)field_mutableSourceRoots.get(sourceRootsContainer)
                    def iterator = mutableSourceRoots.iterator()
                    def pendingAddSource = new ArrayList<File>()
                    while (iterator.hasNext()){
                        def sourceRoot = iterator.next()
                        if(sourceRoot.absolutePath.endsWith("src/main/java")){
                            println "============================ kotlin sourceRoot found for kotlin in mutableSourceRoots: $sourceRoot.absolutePath"
                            iterator.remove()


                            if(!hasDoClean){
                                hasDoClean = true
                                def cleanDir = new File(cleanDirPath)
                                cleanDir.mkdirs()
                                FileUtils.copyDirectory(sourceRoot, cleanDir)
                                doClean(cleanDir, prefixList)
                                pendingAddSource.add(cleanDir)
                            }
                        }
                    }
                    mutableSourceRoots.addAll(pendingAddSource)

                    def field_source = SourceTask.class.getDeclaredField("source")
                    field_source.setAccessible(true)
                    def source = (List<Object>)field_source.get(compileKotlinTask)
                    iterator = source.iterator()
                    while (iterator.hasNext()){
                        def singleSource = iterator.next()
                        if(singleSource instanceof File){
                            if(singleSource.absolutePath.endsWith("src/main/java")){
                                iterator.remove()
                            }
                        } else if(singleSource instanceof DefaultSourceDirectorySet){
                            iterator.remove()
                        }
                    }

                    source.add(new File(cleanDirPath))
                }

                //hook javac compile
                Task compileJavaTask = project.tasks.getByName("compile${variantName}JavaWithJavac")
                compileJavaTask.doFirst {
                    if(compileJavaTask instanceof AndroidJavaCompile){
                        def androidJavaCompile = (AndroidJavaCompile) compileJavaTask
                        println androidJavaCompile.getProcessorListFile()

                        def sourceFileTreesField = AndroidJavaCompile.class.getDeclaredField("sourceFileTrees")
                        sourceFileTreesField.accessible = true
                        def originSourceFileTree = sourceFileTreesField.get(androidJavaCompile)
                        def origin = originSourceFileTree.invoke()
                        def delegateSourceFileTree = new DelegateSourceFileTree(origin, cleanDirPath)

                        sourceFileTreesField.set(androidJavaCompile, delegateSourceFileTree)

                        def sourceField = SourceTask.class.getDeclaredField("source")
                        sourceField.setAccessible(true)
                        def source = (List<DefaultConfigurableFileTree>)sourceField.get(androidJavaCompile)
                        def iterator = source.iterator()
                        while(iterator.hasNext()){
                            def fileTree = iterator.next()
                            if(fileTree instanceof DefaultConfigurableFileTree) {
                                if (fileTree.dir.absolutePath.endsWith("src/main/java")) {  //replace source java dir
                                    def cleanDir = new File(cleanDirPath)

                                    if(!hasDoClean) {
                                        hasDoClean = true
                                        cleanDir.mkdirs()
                                        FileUtils.copyDirectory(fileTree.dir, cleanDir)
                                        doClean(cleanDir, prefixList)
                                    }

                                    fileTree.from(cleanDir)
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