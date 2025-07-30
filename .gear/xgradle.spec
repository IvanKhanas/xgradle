%define _unpackaged_files_terminate_build 1
%def_without check

Name: xgradle
Version: 0.0.1
Release: alt1

Summary: Gradle plugin for building with system artifacts
License: Apache-2.0
Group: Development/Java
Vcs: https://altlinux.space/ALTLinux/xgradle
ExcludeArch: i586

Source0: %name-%version.tar

BuildRequires(pre): rpm-macros-java
BuildRequires(pre): rpm-macros-gradle
BuildRequires: /proc
BuildRequires: rpm-build-java-osgi
BuildRequires: java-21-openjdk-devel
BuildRequires: gradle
Requires: gradle

%if_with check
BuildRequires: xgradle
BuildRequires: junit5
BuildRequires: apiguardian
BuildRequires: gradle-maven-publish-plugin
BuildRequires: google-gson
BuildRequires: apache-commons-io
BuildRequires: apache-commons-cli
%endif

%description
xgradle is a plugin for the gradle build system that allows you to build
java projects using system artifacts.

The plugin uses the pom files contained in your system for artifacts. Actually,
as well as the artifacts themselves. The principle of the plugin is based on
parsing pom files and further searching for the necessary artifacts according
to the received metadata.

%prep
%setup

%build
%gradle_build \
  -x test \
  #

%install
install -Dm 644 build/xgradle-plugin.gradle \
  -t %buildroot%_javadir/gradle/init.d

install -Dm 644 build/libs/xgradle.jar \
  -t %buildroot%_javadir/gradle/lib/plugins

%check
%gradle_check

%files
%_javadir/gradle/init.d/xgradle-plugin.gradle
%_javadir/gradle/lib/plugins/xgradle.jar

%changelog
* Wed Jul 30 2025 Ivan Khanas <xeno@altlinux.org> 0.0.1-alt1
- First build for ALT.
