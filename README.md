# Google Cloud Messaging

Google Cloud Messaging for Android (GCM) is a service that helps developers send
data from servers to their Android applications on Android devices. The service
provides a simple, lightweight mechanism that servers can use to tell mobile
applications to contact the server directly, to fetch updated application or
user data. The GCM service handles all aspects of queueing of messages and
delivery to the target Android application running on the target device.

This project contains an app that demonstrates the GCM APIs for registering,
sending downstream, upstream and notification messages, subscribing to topics,
creating notification groups and using the network manager to schedule tasks.

More information on GCM, including an overview and integration instructions, can
be found on the Android Developers website:

http://developer.android.com/guide/google/gcm/index.html

## Getting Started

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.
To use the app you will need a sender id to be able to register and test client
side APIs, and an API key to be able to test server side APIs. Refer to the
guidelines available at https://developer.android.com/google/gcm/gs.html.

## Screenshots

![Menu](gcm-demo-app/screenshots/menu.png "The app's menu")
![Registration](gcm-demo-app/screenshots/registration1.png "Registration page - unregistered")
![Registration - registered](gcm-demo-app/screenshots/registration2.png "Registration page - registered")
![Downstream](gcm-demo-app/screenshots/downstream.png "Sending downstream messages")
![Groups](gcm-demo-app/screenshots/groups.png "Groups page")
![Topics](gcm-demo-app/screenshots/topics.png "Topics page")
![Add topic](gcm-demo-app/screenshots/add_topic.png "Add a topic")
![Tasks](gcm-demo-app/screenshots/tasks.png "Network manager tasks page")

## Support

- Google+ Community: https://plus.sandbox.google.com/communities/105153134372062985968
- Stack Overflow: http://stackoverflow.com/questions/tagged/android-gcm

If you've found an error in this project's code, please file an issue:
https://github.com/google/gcm/issues

## License

Copyright 2015 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
