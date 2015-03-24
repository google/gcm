# Google Cloud Messaging

Google Cloud Messaging for Android (GCM) is a service that helps developers send
data from servers to their Android applications on Android devices. The service
provides a simple, lightweight mechanism that servers can use to tell mobile
applications to contact the server directly, to fetch updated application or
user data. The GCM service handles all aspects of queueing of messages and
delivery to the target Android application running on the target device.

This project contains sample code and legacy client libraries for interfacing
with GCM. Newer applications should instead include the Google Play Services
library via their Gradle configuration:

    dependencies {
      compile "com.google.android.gms:play-services:3.1.+"
    }

Information on GCM, including an overview and integration instructions, can
be found on the Android Developers website:

http://developer.android.com/guide/google/gcm/index.html

## Support

- Google+ Community: https://plus.sandbox.google.com/communities/105153134372062985968
- Stack Overflow: http://stackoverflow.com/questions/tagged/android-gcm

If you've found an error in this project's code, please file an issue:
https://github.com/google/gcm/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. See CONTRIBUTING.md for more details.

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
