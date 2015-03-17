# Introduction #

This page contains the release notes of all GCM library releases.

# Non-released changes (available only on [source-code repository](https://code.google.com/p/gcm/source/checkout)) #

Bug fixes:
  * Removed redundant check that was logging an internal error message.
  * Minor javadoc fixes.
  * [Issue 5](https://code.google.com/p/gcm/issues/detail?id=5): send() throws NPE if payload has data with null value.
  * [Issue 7](https://code.google.com/p/gcm/issues/detail?id=7): returns empty description when connection.getErrorStream().
  * Fixed orientation settings.
  * Improved usage of PendingIntent that gets package name.
  * Fixed context used to unregister retry receiver.
  * [Issue 9](https://code.google.com/p/gcm/issues/detail?id=9): Changed exception handling logic so failures to communicate with GCM and internal server errors can be retried.

Other changes:

  * Changed workflow so sample app doesn't unregister from GCM in case of error sending registration id to 3rd-party server.
  * Made Builder classes public so they can be used by test cases.
  * Improved logging so tags contain the application name.
  * Removed token used to verify authenticity of retry intent (and use package name instead)
  * Automatically expires registeredOnServer property when registration id is cleared.

# Version 1.0.2 (rev. 3 on SDK) - released on July 31st, 2012 #

Bug fixes:
  * Sender returns body of HTTP response in case of error.
  * [Issue 4](https://code.google.com/p/gcm/issues/detail?id=4): Uses proper intent service class for retry intent

# Version 1.0.1 (rev. 2 on SDK) - released on July 18th, 2012 #

Minor improvements:

  * Added an expiration date for the _isRegisteredOnServer()_ method, and methods to set/get its value.
  * Add a new _getSenderIds()_ method on _GCMBaseIntentService_ so applications can dynamically set the sender ids.
  * Fixed instances where it should take a varags of sender ids instead of a single sender id.
  * Improved demo server by slicing the multicast messages in multiple of 1000s (which is the maximum allowed value).
  * Fixed library to be Java 5 compliant.

# Version 1.0.0 (rev. 1 on SDK)- released on June 27th, 2012 #

Initial version released at Google IO 2012.