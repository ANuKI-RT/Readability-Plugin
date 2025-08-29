### Code Readability Plugin:

This folder contains the source code for the Code Readability Plugin.

The plugin is based on IntelliJ Platform Plugin SDK version "1.17.3"  
[Plugin SDK Documentation](https://plugins.jetbrains.com/docs/intellij/developing-plugins.html).

The implementation was done in Java, using SDK version 17.0.4.

Gradle version 8.7 was used as the build tool.

#### Source Code:

Each source code file is documented with comments. Some features were discarded during the development of the plugin.

The plugin relies on the predictions of the Readability Model by Scalabrino et al. from their publication "A Comprehensive Model for Code Readability" (2018). Since we did not create the Readability Model, it is not included in this repository. Please download the readability model at https://dibt.unimol.it/report/readability/ and add it to a new folder `src/main/resources/readability_model`. The folder is expected to contain two files: the `readability.classifier` and the jar with the name `RSE.jar` (instead of `rsm.jar` in the current download).

There is also a known bug in `MetaDataService.java` that prevents writing the `readability_meta.xml` file. This needs to be fixed in future versions.

Additionally, one plugin feature is now marked as deprecated:  
Specifically, the method `public void projectOpened(Project project)` in the `ProjectManagerListener` class will no longer be available in future versions of the Plugin SDK. An alternative interface will need to be implemented if the plugin is to be updated to work with newer SDK versions.

There is still considerable room for improvement in the plugin. I noted several issues in `TODO` comments that I was unable to address during the project.

It is important to mention that the plugin version used in the study did not contain code comments. These comments were added afterward to facilitate further development. The comments were introduced in commit `48159ed62710b560313bba262e004fb4de94d91c`. The version used in the study is the one prior to this commit.

A fully compiled version of the plugin, as used during the study, is located in the `Survey/pc_preparation` folder. To reproduce the study exactly as it was conducted, this version should be used.

#### Build Process:

The fully functioning plugin requires more than just the compiled Java classes. In addition to this, a temporary folder for code snippets and a folder containing the Scalabrino Readability Model must be included in the final zipped version of the plugin.

These tasks were integrated into the build process in the `build.gradle.kts`. However, there are still some issues, as the plugin is not always built with all dependencies. For some reason, the model folder or the empty temp folder is occasionally missing in the final build.

Future versions should address this and ensure the build process is properly configured.
