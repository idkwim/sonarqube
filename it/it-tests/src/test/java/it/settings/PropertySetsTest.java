/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.settings;

import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.setting.SettingsService;
import org.sonarqube.ws.client.setting.ValuesRequest;
import pageobjects.Navigation;
import pageobjects.settings.SettingsPage;
import util.user.UserRule;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetSettings;

public class PropertySetsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private Navigation nav = Navigation.create(orchestrator);

  static SettingsService SETTINGS;
  private String adminUser;

  @BeforeClass
  public static void initSettingsService() throws Exception {
    SETTINGS = newAdminWsClient(orchestrator).settings();
  }

  @Before
  public void before() {
    adminUser = userRule.createAdminUser();
  }

  @After
  public void reset_settings() throws Exception {
    resetSettings(orchestrator, null, "sonar.demo", "sonar.autogenerated", "sonar.test.jira.servers");
  }

  @Test
  public void support_property_sets() throws UnsupportedEncodingException {
    SettingsPage page = nav.logIn().submitCredentials(adminUser).openSettings(null).openCategory("DEV")
      .assertSettingDisplayed("sonar.test.jira.servers");

    page.getPropertySetInput("sonar.test.jira.servers")
      .setFieldValue("key", "jira1")
      .setFieldValue("url", "http://jira")
      .setFieldValue("port", "12345")
      .save();

    assertPropertySet("sonar.test.jira.servers", asList(
      entry("key", "jira1"),
      entry("url", "http://jira"),
      entry("port", "12345")));
  }

  @Test
  public void support_property_sets_with_auto_generated_keys() throws UnsupportedEncodingException {
    SettingsPage page = nav.logIn().submitCredentials(adminUser).openSettings(null).openCategory("DEV")
      .assertSettingDisplayed("sonar.autogenerated");

    page.getPropertySetInput("sonar.autogenerated")
      .setFieldValue(0, "value", "FIRST")
      .setFieldValue(1, "value", "SECOND")
      .setFieldValue(2, "value", "THIRD")
      .save();

    assertPropertySet("sonar.autogenerated",
      asList(entry("value", "FIRST")),
      asList(entry("value", "SECOND")),
      asList(entry("value", "THIRD")));
  }

  @Test
  public void edit_property_set() {
    SETTINGS.set(SetRequest.builder()
      .setKey("sonar.test.jira.servers")
      .setFieldValues(newArrayList(
        "{\"key\":\"jira1\", \"url\":\"http://jira1\", \"port\":\"12345\"}",
        "{\"key\":\"jira2\", \"url\":\"http://jira2\", \"port\":\"54321\"}"))
      .build());

    assertPropertySet("sonar.test.jira.servers",
      asList(entry("key", "jira1"), entry("url", "http://jira1"), entry("port", "12345")),
      asList(entry("key", "jira2"), entry("url", "http://jira2"), entry("port", "54321")));
  }

  @Test
  public void delete_property_set() throws Exception {
    SETTINGS.set(SetRequest.builder()
      .setKey("sonar.test.jira.servers")
      .setFieldValues(newArrayList("{\"url\":\"http://jira1\"}", "{\"port\":\"12345\"}"))
      .build());

    resetSettings(orchestrator, null, "sonar.test.jira.servers");

    assertThat(SETTINGS.values(ValuesRequest.builder().setKeys("sonar.test.jira.servers").build()).getSettingsList()).isEmpty();
  }

  private void assertPropertySet(String baseSettingKey, List<Map.Entry<String, String>>... fieldsValues) {
    Settings.Setting setting = getSetting(baseSettingKey);
    assertThat(setting.getFieldValues().getFieldValuesList()).hasSize(fieldsValues.length);
    int index = 0;
    for (Settings.FieldValues.Value fieldValue : setting.getFieldValues().getFieldValuesList()) {
      assertThat(fieldValue.getValue()).containsOnly(fieldsValues[index].toArray(new Map.Entry[] {}));
      index++;
    }
  }

  private Settings.Setting getSetting(String key) {
    Settings.ValuesWsResponse response = SETTINGS.values(ValuesRequest.builder().setKeys(key).build());
    List<Settings.Setting> settings = response.getSettingsList();
    assertThat(settings).hasSize(1);
    return settings.get(0);
  }

}
