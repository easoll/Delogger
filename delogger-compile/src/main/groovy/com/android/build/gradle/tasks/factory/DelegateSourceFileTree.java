package com.android.build.gradle.tasks.factory;

import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.DefaultConfigurableFileTree;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.functions.Function0;

/**
 *
 */
public class DelegateSourceFileTree implements Function0<List<FileTree>> {
    private List<FileTree> mOrigin = new ArrayList<>();

    public DelegateSourceFileTree(List<FileTree> origin, String newPath){
        for (FileTree fileTree : origin) {
            mOrigin.add(fileTree);

            if (fileTree.toString().contains("src/main/java") && fileTree instanceof DefaultConfigurableFileTree) {
                ((DefaultConfigurableFileTree)fileTree).setDir(newPath);
            }
        }
    }

    @Override
    public List<FileTree> invoke() {
        return mOrigin;
    }
}
