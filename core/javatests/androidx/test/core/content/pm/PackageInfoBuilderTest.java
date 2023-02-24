/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.test.core.content.pm;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

import android.content.pm.PackageInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link PackageInfoBuilder}. */
@RunWith(AndroidJUnit4.class)
public final class PackageInfoBuilderTest {

  private static final String TEST_PACKAGE_NAME = "test.package.name";
  private static final String SECOND_TEST_PACKAGE_NAME = "second.test.package.name";
  private static final String[] TEST_REQUESTED_PERMISSIONS = {
    "android.permission.test.one", "android.permission.test.two"
  };

  @Test
  public void buildAllFields() {
    PackageInfo packageInfo =
        PackageInfoBuilder.newBuilder()
            .setPackageName(TEST_PACKAGE_NAME)
            .setApplicationInfo(
                ApplicationInfoBuilder.newBuilder().setPackageName(TEST_PACKAGE_NAME).build())
            .setRequestedPermissions(TEST_REQUESTED_PERMISSIONS)
            .build();

    assertThat(packageInfo.packageName).isEqualTo(TEST_PACKAGE_NAME);
    assertThat(packageInfo.applicationInfo).isNotNull();
    assertThat(packageInfo.requestedPermissions).isEqualTo(TEST_REQUESTED_PERMISSIONS);
  }

  @Test
  public void defaultApplicationInfoIsValid() {
    PackageInfoBuilder builder = PackageInfoBuilder.newBuilder().setPackageName(TEST_PACKAGE_NAME);

    PackageInfo packageInfo = builder.build();

    assertThat(packageInfo.applicationInfo).isNotNull();
    assertThat(packageInfo.applicationInfo.packageName).isEqualTo(packageInfo.packageName);
  }

  @Test
  public void build_throwsException_whenPackageNameMissing() throws Exception {
    try {
      PackageInfoBuilder.newBuilder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat().isEqualTo("Mandatory field 'packageName' missing.");
    }
  }

  @Test
  public void build_throwsException_whenPackageNameMismatched() {
    PackageInfoBuilder builder =
        PackageInfoBuilder.newBuilder()
            .setPackageName(TEST_PACKAGE_NAME)
            .setApplicationInfo(
                ApplicationInfoBuilder.newBuilder()
                    .setPackageName(SECOND_TEST_PACKAGE_NAME)
                    .build());

    try {
      builder.build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e)
          .hasMessageThat()
          .isEqualTo("Field 'packageName' must match field 'applicationInfo.packageName'");
    }
  }
}
