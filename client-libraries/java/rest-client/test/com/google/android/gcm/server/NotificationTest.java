/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gcm.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTest {

  @Test
  public void testRequiredParameters() {
    Notification notification = new Notification.Builder("myicon").build();
    assertNull(notification.getTitle());
    assertNull(notification.getBody());
    assertEquals("myicon", notification.getIcon());
    assertEquals("default", notification.getSound());
    assertNull(notification.getBadge());
    assertNull(notification.getTag());
    assertNull(notification.getColor());
    assertNull(notification.getClickAction());
    assertNull(notification.getBodyLocKey());
    assertNull(notification.getBodyLocArgs());
    assertNull(notification.getTitleLocKey());
    assertNull(notification.getTitleLocArgs());
    String toString = notification.toString();
    assertFalse(toString.contains("title"));
    assertFalse(toString.contains("body"));
    assertTrue(toString.contains("icon=myicon"));
    assertTrue(toString.contains("sound=default"));
    assertFalse(toString.contains("badge"));
    assertFalse(toString.contains("tag"));
    assertFalse(toString.contains("color"));
    assertFalse(toString.contains("clickAction"));
    assertFalse(toString.contains("bodyLocKey"));
    assertFalse(toString.contains("bodyLocArgs"));
    assertFalse(toString.contains("titleLocKey"));
    assertFalse(toString.contains("titleLocArgs"));
  }

  @Test
  public void testOptionalParameters() {
    Notification notification = new Notification.Builder("ico")
            .title("Hi")
            .body("Hello world!")
            .sound("notDefault")
            .badge(3)
            .tag("tagged")
            .color("#ffffff")
            .clickAction("OPEN_ACTIVITY_1")
            .bodyLocKey("GREETING_BODY_FORMAT")
            .bodyLocArgs(Arrays.asList("first", "second"))
            .titleLocKey("GREETING_TITLE_FORMAT")
            .titleLocArgs(Arrays.asList("one", "two", "three"))
            .build();
    assertEquals("Hi", notification.getTitle());
    assertEquals("Hello world!", notification.getBody());
    assertEquals("ico", notification.getIcon());
    assertEquals("notDefault", notification.getSound());
    assertTrue(notification.getBadge() == 3);
    assertEquals("tagged", notification.getTag());
    assertEquals("#ffffff", notification.getColor());
    assertEquals("OPEN_ACTIVITY_1", notification.getClickAction());
    assertEquals("GREETING_BODY_FORMAT", notification.getBodyLocKey());
    assertTrue(notification.getBodyLocArgs().contains("first"));
    assertTrue(notification.getBodyLocArgs().contains("second"));
    assertTrue(notification.getBodyLocArgs().size() == 2);
    assertEquals("GREETING_TITLE_FORMAT", notification.getTitleLocKey());
    assertTrue(notification.getTitleLocArgs().contains("one"));
    assertTrue(notification.getTitleLocArgs().contains("two"));
    assertTrue(notification.getTitleLocArgs().contains("three"));
    assertTrue(notification.getTitleLocArgs().size() == 3);
    String toString = notification.toString();
    assertTrue(toString.contains("title=Hi"));
    assertTrue(toString.contains("body=Hello world!"));
    assertTrue(toString.contains("sound=notDefault"));
    assertTrue(toString.contains("badge=3"));
    assertTrue(toString.contains("tag=tagged"));
    assertTrue(toString.contains("color=#ffffff"));
    assertTrue(toString.contains("clickAction=OPEN_ACTIVITY_1"));
    assertTrue(toString.contains("bodyLocKey=GREETING_BODY_FORMAT"));
    assertTrue(toString.contains("bodyLocArgs=[first, second]"));
    assertTrue(toString.contains("titleLocKey=GREETING_TITLE_FORMAT"));
    assertTrue(toString.contains("titleLocArgs=[one, two, three]"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testBodyLocArgsDataIsImmutable() {
    Notification.Builder builder = new Notification.Builder("myicon");
    builder.bodyLocArgs(new ArrayList<String>());
    Notification notification = builder.build();
    notification.getBodyLocArgs().clear();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTitleLocArgsDataIsImmutable() {
    Notification.Builder builder = new Notification.Builder("myicon");
    builder.titleLocArgs(new ArrayList<String>());
    Notification notification = builder.build();
    notification.getTitleLocArgs().clear();
  }
}
