package org.oppia.android.data.backends.test

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.Headers
import okhttp3.Request
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.oppia.android.data.backends.api.MockFeedbackReportingService
import org.oppia.android.data.backends.gae.NetworkModule
import org.oppia.android.data.backends.gae.RemoteAuthNetworkInterceptor
import org.robolectric.annotation.LooperMode
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** Tests for [RemoteAuthNetworkInterceptor] */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RemoteAuthNetworkInterceptorTest {

  @Inject
  lateinit var networkInterceptor: RemoteAuthNetworkInterceptor

//  @Mock lateinit var mockRequest: Request

  @Before
  fun setUp() {
    setUpTestApplicationComponent()
//    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun testNetworkInterceptor_withoutHeaders_addsCorrectHeaders() {
    val request = Request.Builder().url("fake_url").build()
    assertThat(request.header("api_key")).isNull()
    assertThat(request.header("app_package_name")).isNull()
    assertThat(request.header("app_version_name")).isNull()
    assertThat(request.header("app_version_code")).isNull()

    val newRequest = networkInterceptor.addAuthHeaders(request)

    assertThat(newRequest.header("api_key")).isNotNull()
    assertThat(newRequest.header("app_package_name")).isNotNull()
    assertThat(newRequest.header("app_version_name")).isNotNull()
    assertThat(newRequest.header("app_version_code")).isNotNull()
  }

  @Test
  fun testNetworkInterceptor_withHeaders_setsCorrectHeaders() {
    val request = Request.Builder().url("fake_url")
      .addHeader("api_key", "wrong_api_key")
      .addHeader("app_package_name", "wrong_package_name")
      .addHeader("app_version_name", "wrong_version_name")
      .addHeader("app_version_code", "wrong_version_code")
      .build()

    val newRequest = networkInterceptor.addAuthHeaders(request)

    assertThat(newRequest.header("api_key")).isNotEqualTo("wrong_api_key")
    assertThat(newRequest.header("app_package_name"))
      .isNotEqualTo("wrong_package_name")
    assertThat(newRequest.header("app_version_name"))
      .isNotEqualTo("wrong_version_name")
    assertThat(newRequest.header("app_version_code"))
      .isNotEqualTo("wrong_version_code")
  }

  private fun setUpTestApplicationComponent() {
    DaggerRemoteAuthNetworkInterceptorTest_TestApplicationComponent.builder()
      .setApplication(ApplicationProvider.getApplicationContext())
      .build()
      .inject(this)
  }

  @Qualifier
  annotation class OppiaRetrofit

  // TODO(#89): Move this to a common test application component.
  @Module
  class TestNetworkModule {
    @Provides
    @Singleton
    fun provideMockFeedbackReportingService(
      @OppiaRetrofit retrofit: Retrofit
    ): MockFeedbackReportingService {
      return retrofit.create(MockFeedbackReportingService::class.java)
    }
  }

  // TODO(#89): Move this to a common test application component.
  @Singleton
  @Component(modules = [NetworkModule::class])
  interface TestApplicationComponent {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder

      fun build(): TestApplicationComponent
    }

    fun inject(networkInterceptorTest: RemoteAuthNetworkInterceptorTest)
    fun inject(networkInterceptor: RemoteAuthNetworkInterceptor)
  }
}
