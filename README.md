
 Copyright 2015 Tampere University of Technology, Pori Department
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
   http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.


Edit the file for build.properties to fit your use case.

You need to manually include in your build path the tools.jar generally found
in the Java JDK installation path (lib directory).

All other required .jar files are in the lib directory.

Importing the source code as existing ant project to eclipse will probably not
work. It is better to create a new java project and select the project directory
to be the source code location (javadocer), and set the required build path
variables manually.
