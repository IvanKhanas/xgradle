```
                             _ _
    __  ____ _ _ __ __ _  __| | | ___
    \ \/ / _` | '__/ _` |/ _` | |/ _ \
     >  < (_| | | | (_| | (_| | |  __/
    /_/\_\__, |_|  \__,_|\__,_|_|\___| v 0.0.1
         |___/
```

### 🤔 What`s xgradle?

**xgradle** is a plugin for the gradle build system that allows you to build
java projects using system artifacts

The plugin uses the pom files contained in your system for artifacts. Actually,
as well as the artifacts themselves. The principle of the plugin is based on
parsing pom files and further searching for the necessary artifacts according
to the received metadata.

---
### 🌟 Key Features

• **System Dependency Resolution** - Uses artifacts from system directories

• **Auto-versioning** - Automatically determines dependency versions

• **Plugin Management** - Supports system-installed Gradle plugins

• **BOM Support** - Full Bill-of-Materials management

• **Transitive Dependencies** - Automatic resolution of dependency chains

• **Offline Capable** - Works without internet access

---
### 🛠 Configuration

Build the plugin based on your architecture

Set these properties to directories with the corresponding files

```-Djava.library.dir``` : Displays a directory with JAR files

```-Dmaven.poms.dir``` : Displays a directory with POM files

**Example**

```
gradle build \
  -Djava.library.dir=/path/to/your/jars \
  -Dmaven.poms.dir=/path/to/your/poms \
  #
```

The following properties affect the interface

```-Ddisable.logo=true``` : Disables ASCII startup banner

```-Ddisable.ansi.color=true``` : Disables colored output

---
### 📂 Standard Installation Method

**1.Build the plugin**
```
gradle build \
  -Djava.library.dir=/path/to/your/jars \
  -Dmaven.poms.dir=/path/to/your/poms \
  #
```

**2.Move the plugin`s init script to the folder with the gradle init scripts**
```
mv build/xgradle-plugin.gradle /path/to/gradle/init.d
```

**3.Move the plugin`s jar file to the gradle plugins directory**
```
mv build/libs/xgradle.jar /path/to/gradle/xgradle
```

---
### 📜 License

Apache 2.0 - See [LICENSE](LICENSE)

```
Copyright 2025 BaseALT Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---
### 🌱 Contributing

**xgradle is a community project. We welcome new members!**

The easiest way to contribute is to submit pull requests and issues to the
Github repository.

You can also write directly to the xgradle  [maintainers](mailto:xeno@altlinux.org).
