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

package com.netflix.kayenta.atlas.model;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AtlasResults {

  @NotNull
  @Getter
  private String type;

  @NotNull
  @Getter
  // TODO(duftler): Might be better to have a different model for results that have messages and no data.
  private String message;

  @NotNull
  @Getter
  private String id;

  @NotNull
  @Getter
  private String query;

  @NotNull
  @Getter
  private String label;

  @NotNull
  @Getter
  private long start;

  @NotNull
  @Getter
  private long step;

  @NotNull
  @Getter
  private long end;

  @NotNull
  @Getter
  private Map<String, String> tags;

  @NotNull
  @Getter
  private TimeseriesData data;

  public List<String> keysFromLabel() {
    Pattern pattern = Pattern.compile("\\(([a-z0-9\\.]+)=[^\\)]+\\)");
    ArrayList<String> ret = new ArrayList<>();
    Matcher matcher = pattern.matcher(label);
    while (matcher.find()) {
      ret.add(matcher.group(1));
    }

    return ret;
  }

}
