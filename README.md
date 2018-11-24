# Delogger
一个用于移除代码中Log语句的android编译插件。
## 效果展示
在使用插件前后，分别有一下日志输出：

![log_before](./img/log_before.png)

![log_after](./img/log_after.png)

## 使用方法
project的build.gradle

```
buildscript {
    repositories {
        maven{
            url "https://dl.bintray.com/easoll/android-plugin"
        }
    }
    dependencies {
        classpath 'com.easoll.plugin:delogger:1.0.3'
    }
}
```

app的build.gradle
```
apply plugin: 'com.easoll.delogger'
```

## 原理介绍
[Android代码清理插件-Delogger](https://easoll.github.io/2018/11/23/Android%E4%BB%A3%E7%A0%81%E6%B8%85%E7%90%86%E6%8F%92%E4%BB%B6-Delogger/)