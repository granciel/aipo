/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.comet.CometProcessor;

import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.orm.Database;

/**
 *
 */
public class ALCometServlet extends HttpServlet implements CometProcessor {

  private static final long serialVersionUID = -3319969086920456957L;

  public static final String KEY_MESSAGE_SENDER =
    "com.aimluck.eip.http.ALCometServlet.MessageSender";

  protected transient ConcurrentHashMap<HttpServletResponse, String> connections =
    null;

  protected transient MessageSender sender = null;

  @Override
  public void init() {
    connections = new ConcurrentHashMap<HttpServletResponse, String>();

    this.sender = new MessageSender();
    this.sender.setDaemon(true);
    this.sender.start();

    getServletContext().setAttribute(KEY_MESSAGE_SENDER, sender);

  }

  @Override
  public void destroy() {
    sender.quit();
    sender = null;
    connections.clear();
  }

  /**
   * @param event
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void event(CometEvent event) throws IOException, ServletException {

    try {
      HttpServletRequest request = event.getHttpServletRequest();
      HttpServletResponse response = event.getHttpServletResponse();
      ALBaseUser user = getUser(request);
      if (user == null) {
        close(response);
        event.close();
        return;
      }
      switch (event.getEventType()) {
        case BEGIN:
          open(request, response);
          event.setTimeout(60 * 60 * 1000);
          break;
        case READ:
          open(request, response);
          event.setTimeout(60 * 60 * 1000);
          break;
        case END:
          close(response);
          event.close();
          break;
        case ERROR:
          close(response);
          event.close();
          break;
        default:
          break;
      }
    } catch (Throwable t) {
      log(t.getMessage(), t);
    }
  }

  protected void open(HttpServletRequest request, HttpServletResponse response) {
    ALBaseUser user = getUser(request);
    if (user != null) {
      connections.put(response, user.getUserName());
    }
  }

  protected void close(HttpServletResponse response) {
    try {
      if (!response.isCommitted()) {
        response.setContentType("text/json");
        response.setCharacterEncoding("UTF-8");
        ServletOutputStream os = response.getOutputStream();
        os.write("{}".getBytes(ALEipConstants.DEF_CONTENT_ENCODING));
        os.flush();
        os.close();
      }
    } catch (Throwable t) {
      log(t.getMessage(), t);
    } finally {
      try {
        connections.remove(response);
      } catch (Throwable ignore) {
        //
      }
    }
  }

  protected ALBaseUser getUser(HttpServletRequest httpServletRequest) {
    try {
      HttpSession session = httpServletRequest.getSession(false);
      if (session == null) {
        return null;
      }
      return (ALBaseUser) session.getAttribute("turbine.user");
    } catch (Throwable ignore) {
      // ignore
    }
    return null;
  }

  public static class Message {

    private final List<String> recipients;

    private final String message;

    public Message(List<String> recipients, String message) {
      this.recipients = recipients;
      this.message = message;
    }

    /**
     * @return message
     */
    public String getMessage() {
      return message;
    }

    /**
     * @return recipients
     */
    public List<String> getRecipients() {
      return recipients;
    }

  }

  public class MessageSender extends Thread {

    protected final List<Message> messages = new ArrayList<Message>();

    protected boolean running = true;

    public void quit() {
      running = false;
      messages.clear();
      this.interrupt();
    }

    public synchronized void sendMessage(List<String> recipients, String message) {
      try {
        synchronized (messages) {
          messages.add(new Message(recipients, message));
          messages.notify();
        }
      } catch (Throwable ignore) {
        //
      }
    }

    @Override
    public void run() {
      while (running) {
        try {
          if (messages.size() == 0) {
            try {
              synchronized (messages) {
                messages.wait();
              }
            } catch (InterruptedException ignore) {
              // Ignore
            }
          }
          Message[] pendingMessages = null;
          synchronized (messages) {
            pendingMessages = messages.toArray(new Message[0]);
            messages.clear();
          }
          for (Message message : pendingMessages) {
            Iterator<Entry<HttpServletResponse, String>> iterator =
              connections.entrySet().iterator();

            while (iterator.hasNext()) {
              Entry<HttpServletResponse, String> next = iterator.next();
              if (message.getRecipients().contains(next.getValue())
                || message.getRecipients().contains(Database.DEFAULT_ORG)) {
                HttpServletResponse response = next.getKey();
                try {
                  if (!response.isCommitted()) {
                    response.setContentType("text/json");
                    response.setCharacterEncoding("UTF-8");
                    ServletOutputStream os = response.getOutputStream();
                    os.write(message.getMessage().getBytes(
                      ALEipConstants.DEF_CONTENT_ENCODING));
                    os.flush();
                    os.close();
                  }
                } catch (Throwable t) {
                  log(t.getMessage(), t);
                } finally {
                  try {
                    connections.remove(response);
                  } catch (Throwable ignore) {
                    //
                  }
                }
              }
            }
            pendingMessages = null;
          }
          try {
            Thread.sleep(100);
          } catch (Throwable ignore) {
            //
          }
        } catch (Throwable t) {
          log(t.getMessage(), t);
          //
        }
      }
    }
  }
}
