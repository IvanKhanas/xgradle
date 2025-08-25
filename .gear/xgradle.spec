%define _unpackaged_files_terminate_build 1
%def_with check

Name: xgradle
Version: 0.0.2
Release: alt1

Summary: Gradle plugin for system dependency resolution and offline builds
License: Apache-2.0
Group: Development/Java
Url: https://github.com/IvanKhanas/xgradle
Vcs: https://github.com/IvanKhanas/xgradle.git
ExclusiveArch: %java_arches

Source0: %name-%version.tar
Source1: %name-stage1.tar

BuildRequires(pre): rpm-macros-java
BuildRequires: /proc
BuildRequires: gradle
BuildRequires: rpm-build-java-osgi
BuildRequires: java-17-openjdk-devel
BuildRequires: maven-lib
BuildRequires: plexus-utils
BuildRequires: shadow-gradle-plugin

%if_with check
BuildRequires: junit5
BuildRequires: apiguardian
BuildRequires: apache-commons-io
BuildRequires: apache-commons-cli
BuildRequires: google-gson
BuildRequires: gradle-maven-publish-plugin
%endif

%package javadoc
Summary: API documentation for XGradle
Group: Development/Java

%description
XGradle is a custom Gradle plugin that provides enhanced dependency resolution
capabilities using system-installed artifacts rather than remote repositories.
It handles both regular dependencies and Gradle plugins, supports BOM (Bill of
Materials) packages, and enables fully offline builds by leveraging locally
available JAR files and POM metadata. The plugin automatically resolves version
conflicts, manages transitive dependencies, and provides detailed logging
throughout the resolution process.

%description javadoc
Javadoc documentation for XGradle Gradle plugin system. Contains API reference
and developer documentation for implementing custom dependency resolution
systems using XGradle plugin infrastructure.

%prep
%setup -a1

XGRADLE_BUILD_DIR="$PWD/xgradle-stage1/build/libs"

sed -i "s|gradle.gradleHomeDir.getAbsolutePath() + \"/lib/plugins\"|\"$XGRADLE_BUILD_DIR\"|g" \
  xgradle-stage1/src/main/resources/xgradle-plugin.gradle

%build

pushd xgradle-stage1
gradle build -x check
popd

gradle build \
  --init-script xgradle-stage1/build/xgradle-plugin.gradle \
  -Djava.library.dir=%_javadir \
  -Dmaven.poms.dir=%_mavenpomdir \
  -Ddisable.ansi.color=true \
  -x check \
  #

%install
install -Dm 644 build/dist/xgradle.jar \
  -t %buildroot%_datadir/gradle/xgradle

install -Dm 644 build/dist/xgradle-plugin.gradle \
 -t %buildroot%_datadir/gradle/init.d

install -Dm 644 build/dist/xgradle.pom \
  -t %buildroot%_mavenpomdir/xgradle

install -Dm 644 build/dist/xgradle-javadoc.jar \
  -t %buildroot%_javadocdir/xgradle

install -d %buildroot%_javadir/xgradle

ln -s %_datadir/gradle/xgradle/xgradle.jar \
  -t %buildroot%_javadir/xgradle

%check
gradle check \
  --init-script xgradle-stage1/build/xgradle-plugin.gradle \
  -Djava.library.dir=%_javadir \
  -Dmaven.poms.dir=%_mavenpomdir \
  -Ddisable.ansi.color=true \
  #

%files
%_javadir/xgradle/xgradle.jar
%_datadir/gradle/xgradle/xgradle.jar
%_datadir/gradle/init.d/xgradle-plugin.gradle
%_mavenpomdir/xgradle/xgradle.pom

%files javadoc
%doc LICENSE README.md
%_javadocdir/xgradle/xgradle-javadoc.jar

%changelog
* Mon Aug 25 2025 Ivan Khanas <xeno@altlinux.org> 0.0.2-alt1
- First build for ALT Linux.

