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
package com.google.android.gcm.demo.server;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
@SuppressWarnings("serial")
public class SendAllMessagesServlet extends BaseServlet {

  private Sender sender;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    sender = newSender(config);
  }

  /**
   * Creates the {@link Sender} based on the servlet settings.
   */
  protected Sender newSender(ServletConfig config) {
    String key = (String) config.getServletContext()
        .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
    return new Sender(key);
  }

  /**
   * Processes the request to add a new message.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    List<String> devices = Datastore.getDevices();
    StringBuilder status = new StringBuilder();
    if (devices.isEmpty()) {
      status.append("Message ignored as there is no device registered!");
    } else {
      List<Result> results;
      // NOTE: check below is for demonstration purposes; a real application
      // could always send a multicast, even for just one recipient
      if (devices.size() == 1) {
        // send a single message using plain post
        String registrationId = devices.get(0);
        Message message = new Message.Builder().build();
        Result result = sender.send(message, registrationId, 5);
        results = Arrays.asList(result);
      } else {
        // send a multicast message using JSON
        Message message = new Message.Builder().build();
        MulticastResult result = sender.send(message, devices, 5);
        results = result.getResults();
      }
      // analyze the results
      for (int i = 0; i < devices.size(); i++) {
        Result result = results.get(i);
        if (result.getMessageId() != null) {
          status.append("Succesfully sent message to device #").append(i);
          String canonicalRegId = result.getCanonicalRegistrationId();
          if (canonicalRegId != null) {
            // same device has more than on registration id: update it
            logger.finest("canonicalRegId " + canonicalRegId);
            devices.set(i, canonicalRegId);
            status.append(". Also updated registration id!");
          }
        } else {
          String error = result.getErrorCodeName();
          if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
            // application has been removed from device - unregister it
            status.append("Unregistered device #").append(i);
            String regId = devices.get(i);
            Datastore.unregister(regId);
          } else {
            status.append("Error sending message to device #").append(i)
                .append(": ").append(error);
          }
        }
        status.append("<br/>");
      }
    }
    req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, status.toString());
    getServletContext().getRequestDispatcher("/home").forward(req, resp);
  }

}
