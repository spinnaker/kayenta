/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.orca.controllers;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.validation.ValidationException;
import com.netflix.discovery.StatusChangeEvent;
import com.netflix.spinnaker.kork.eureka.RemoteStatusChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;

@RestController
@RequestMapping("/admin/orca")
public class AdminController {

  @Autowired
  ApplicationEventPublisher publisher;

  @RequestMapping(value = "/instance/enabled", consumes = "application/json", method = RequestMethod.POST)
  void setInstanceEnabled(@RequestBody Map<String, Boolean> enabledWrapper) {
    Boolean enabled = enabledWrapper.get("enabled");

    if (enabled == null) {
      throw new ValidationException("The field 'enabled' must be set.", null);
    }

    setInstanceEnabled(enabled);
  }

  private void setInstanceEnabled(boolean enabled) {
    InstanceInfo.InstanceStatus currentStatus = enabled ? UP : OUT_OF_SERVICE;
    InstanceInfo.InstanceStatus previousStatus = currentStatus == OUT_OF_SERVICE ? UP : UNKNOWN;

    publisher.publishEvent(new RemoteStatusChangedEvent(new StatusChangeEvent(previousStatus, currentStatus)));
  }
}
