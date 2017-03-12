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
package com.aimluck.eip.services.config;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;

/**
 *
 */
public abstract class ALConfigHandler {

  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALConfigHandler.class.getName());

  public abstract String get(Property property);

  public abstract void put(Property property, String value);

  public abstract String get(String property, String defaultValue);

  public abstract void put(String property, String value);

  public static enum Property {

    ACCESS_GLOBAL_URL_PROTOCOL("access.global.url.protocol") {

      @Override
      public String defaultValue() {
        return JetspeedResources.getString("access.url.protocol", "http");
      }
    },

    ACCESS_LOCAL_URL_PROTOCOL("access.local.url.protocol") {

      @Override
      public String defaultValue() {
        return JetspeedResources.getString("access.url.protocol", "http");
      }
    },

    MINIMUM_ADMINISTRATOR_USER_COUNT("minimum.administrator.user.count") {

      @Override
      public String defaultValue() {
        return "0";
      }
    },

    EXTERNAL_RESOURCES_URL("external.resources.url") {

      @Override
      public String defaultValue() {
        return JetspeedResources.getString("external.resources.url", "");
      }
    },

    EXTERNAL_LOGIN_URL("external.login.url") {

      @Override
      public String defaultValue() {
        return JetspeedResources.getString("external.login.url", "");
      }
    },

    CHECK_ACTIVITY_URL("check.activity.url") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString("check.activity.url", "");
      }
    },

    CHECK_ACTIVITY_RELAY_URL("check.activity.relay.url") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString(
          "check.activity.relay.url",
          "/gadgets/files/container/rpc_relay.html");
      }
    },

    CHECK_ACTIVITY_INTERVAL("check.activity.interval") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString("check.activity.interval", "300");
      }
    },

    EXTTIMECARD_VERTION("exttimecard.version") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString("exttimecard.version", "1");
      }
    },

    SCHEDULE_MAPS_ENABLED("config.schedule.maps.enabled") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString("config.schedule.maps.enabled", "F");
      }
    },

    HOLIDAY_OF_WEEK("holiday.of.week") {
      @Override
      public String defaultValue() {
        // 7桁 週間の休日
        // 1桁 法定休日の曜日
        // 1桁 国民の祝日を休日にするかどうか
        return JetspeedResources.getString("holiday.of.week", "100000111");

      }
    },

    FIRST_ADMIN_LOGIN("first.admin.login") {
      @Override
      public String defaultValue() {
        return JetspeedResources.getString("first.admin.login", "T");
      }
    };

    private final String property;

    private Property(String property) {
      this.property = property;
    }

    @Override
    public String toString() {
      return this.property;
    }

    public abstract String defaultValue();
  }

}
