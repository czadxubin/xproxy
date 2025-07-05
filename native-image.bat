@rem 准备工作预生成native构建的一些配置文件
@rem java -agentlib:native-image-agent=config-output-dir=native-config  -jar target/xproxy-1.0-SNAPSHOT-jar-with-dependencies.jar

@Rem 加载x64编译环境
call "E:\win11Software\Microsoft Visual Studio\2022\VC\Auxiliary\Build\vcvarsall.bat" x64


@Rem 设置JAVA_HOME
set JAVA_HOME "E:\Java\graalvm-jdk-21.0.7+8.1"

@rem 创建二进制镜像
E:\Java\graalvm-jdk-21.0.7+8.1\bin\java.exe -Dmaven.multiModuleProjectDirectory=E:\Codes\xproxy -Djansi.passthrough=true "-Dmaven.home=E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3" "-Dclassworlds.conf=E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3\bin\m2.conf" "-Dmaven.ext.class.path=E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven-event-listener.jar" "-javaagent:E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\lib\idea_rt.jar=64311:E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath "E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3\boot\plexus-classworlds-2.7.0.jar;E:\win11Software\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3\boot\plexus-classworlds.license" org.codehaus.classworlds.Launcher -Didea.version=2024.1.1 -s E:\win11Software\maven\apache-maven-3.9.5\conf\settings.xml -DskipTests=true package -X -P native
