

# README

## 编译环境

* IDE

  ```
  IntelliJ IDEA 2020.2.3 (Ultimate Edition)
  Build #IU-202.7660.26, built on October 6, 2020
  Licensed to https://zhile.io
  You have a perpetual fallback license for this version
  Subscription is active until July 8, 2089
  Runtime version: 11.0.8+10-b944.34 amd64
  VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
  Windows 10 10.0
  GC: ParNew, ConcurrentMarkSweep
  Memory: 1965M
  Cores: 8
  Registry: ide.suppress.double.click.handler=true
  Non-Bundled Plugins: Statistic, org.jetbrains.kotlin, com.intellij.plugins.html.instantEditing, com.jetbrains.php, Pythonid
  ```

* JAVA version 

  ```
  java version "1.8.0_271"
  Java(TM) SE Runtime Environment (build 1.8.0_271-b09)
  Java HotSpot(TM) 64-Bit Server VM (build 25.271-b09, mixed mode)
  ```

* OS

  ```
  Microsoft Window 10 家庭中文版
  基于x64的电脑
  ```

  

## 主要文件及目录说明

```python
./
|-resources/ #服务器资源目录，默认需要拷贝到D:/下
|          |-html/
|          |     |-img/
|          |     |    |-logo.jpg
|          |     |-noimg.html
|          |     |-test.html
|          |-txt/
|               |-test.txt
|-socket/ #工程目录，此目录可直接在IDEA打开
|       |-.idea/*
|       |-out/
|       |    |-production/*
|       |    |-artifacts/
|       |               |-socket_jar/
|       |                           |-socket.jar #每次进行Build jar时，产生的jar将保存在此处
|       |-src/
|            |-META-INF/*
|            |-ClientConnection.java #程序源代码ClientConnection类
|            |-LogPrinter.java #程序源代码LogPrinter类
|            |-MySocket.java #程序源代码Mysocket类 （main）
|            |-RequestParser.java #程序源代码RequestParser类
|            |-socket.iml
|-README.md #本README文件
|-socket.jar #可执行程序 需要在cmd使用java -jar socket.jar运行
|-常博宇_实验8.pdf #实验报告
```



## 运行说明

* 编译：使用`Intellij IDEA`直接打开`socket`目录，即为工程目录。运行`Build`选项卡下的`Build-Artifacts`，即可在`socket/out/artifacts/socket_jar`目录下生成`socket.jar`文件（为方便起见，提交的文件里已经生成了`socket.jar`文件）
* 运行：==首先需要将`resources`目录拷贝到D盘下==（或者修改`Mysocket`类下的`localPath`为新的`resource`路径并编译）推荐使用方法2，因为在方法2中使用`ctrl+c`退出程序时可以看到线程池关闭的相关调试信息。
  * 方法1：直接在`IDEA`中使用`Shift+F10`运行
  * 方法2：使用`java -jar socket.jar`运行`socket.jar`
  * 访问如下网址
    * http://127.0.0.1:2943/txt/test.txt 
    * http://127.0.0.1:2943/html/test.html
    * http://127.0.0.1:2943/html/noimg.html
    * http://127.0.0.1:2943/html/img/logo.jpg



## 其他说明

请不要直接双击运行`socket.jar`，否则使用的2943端口将持续被占用，虽然可以实现服务器交互，但是看不到调试信息，也不方便退出。

如果出现以下报错：

```java
Exception in thread "main" java.net.BindException: Address already in use: JVM_Bind
  at java.net.DualStackPlainSocketImpl.bind0(Native Method)
  at java.net.DualStackPlainSocketImpl.socketBind(DualStackPlainSocketImpl.java:106)
  at java.net.AbstractPlainSocketImpl.bind(AbstractPlainSocketImpl.java:387)
  at java.net.PlainSocketImpl.bind(PlainSocketImpl.java:190)
  at java.net.ServerSocket.bind(ServerSocket.java:375)
  at java.net.ServerSocket.<init>(ServerSocket.java:237)
  at java.net.ServerSocket.<init>(ServerSocket.java:128)
  at MySocket.main(MySocket.java:15)
```

请在cmd中运行`netstat -ano | find "2943"` 找到使用端口进程的id号，然后使用`taskkill /f /pid "xxxx"`结束此进程（xxxx表示此进程的id号）
