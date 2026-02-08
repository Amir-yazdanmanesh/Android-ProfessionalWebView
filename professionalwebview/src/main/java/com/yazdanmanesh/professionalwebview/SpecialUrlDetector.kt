/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yazdanmanesh.professionalwebview

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import com.yazdanmanesh.professionalwebview.SpecialUrlDetector.UrlType
import java.net.URISyntaxException

interface SpecialUrlDetector {
    fun determineType(uri: Uri): UrlType
    fun determineType(uriString: String?): UrlType

    sealed class UrlType {
        data class Web(val webAddress: String) : UrlType()
        data class Telephone(val telephoneNumber: String) : UrlType()
        data class Email(val emailAddress: String) : UrlType()
        data class Sms(val telephoneNumber: String) : UrlType()
        data class AppLink(
            val appIntent: Intent? = null,
            val excludedComponents: List<ComponentName>? = null,
            val uriString: String
        ) : UrlType()

        data class NonHttpAppLink(
            val uriString: String,
            val intent: Intent,
            val fallbackUrl: String?,
            val title: String?,
            val fallbackIntent: Intent? = null
        ) : UrlType()

        data class SearchQuery(val query: String) : UrlType()
        data class Unknown(val uriString: String) : UrlType()
        data class ExtractedTrackingLink(val extractedUrl: String) : UrlType()
    }
}

class SpecialUrlDetectorImpl(
    private val context: Context,
    private val customDeeplinkSchemes: List<DeeplinkConfig> = emptyList()
) : SpecialUrlDetector {

    data class DeeplinkConfig(
        val scheme: String,
        val urlParam: String = "url",
        val titleParam: String = "title"
    )

    override fun determineType(uri: Uri): UrlType {
        val uriString = uri.toString()

        return when (val scheme = uri.scheme) {
            TEL_SCHEME -> buildTelephone(uriString)
            TELPROMPT_SCHEME -> buildTelephonePrompt(uriString)
            MAILTO_SCHEME -> buildEmail(uriString)
            SMS_SCHEME -> buildSms(uriString)
            SMSTO_SCHEME -> buildSmsTo(uriString)
            HTTP_SCHEME, HTTPS_SCHEME, DATA_SCHEME -> processUrl(uriString)
            ABOUT_SCHEME -> UrlType.Unknown(uriString)
            JAVASCRIPT_SCHEME -> UrlType.SearchQuery(uriString)
            null -> UrlType.SearchQuery(uriString)
            else -> checkForIntent(scheme, uriString)
        }
    }

    private fun buildTelephone(uriString: String): UrlType =
        UrlType.Telephone(uriString.removePrefix("$TEL_SCHEME:").truncate(PHONE_MAX_LENGTH))

    private fun buildTelephonePrompt(uriString: String): UrlType =
        UrlType.Telephone(uriString.removePrefix("$TELPROMPT_SCHEME:").truncate(PHONE_MAX_LENGTH))

    private fun buildEmail(uriString: String): UrlType =
        UrlType.Email(uriString.truncate(EMAIL_MAX_LENGTH))

    private fun buildSms(uriString: String): UrlType =
        UrlType.Sms(uriString.removePrefix("$SMS_SCHEME:").truncate(SMS_MAX_LENGTH))

    private fun buildSmsTo(uriString: String): UrlType =
        UrlType.Sms(uriString.removePrefix("$SMSTO_SCHEME:").truncate(SMS_MAX_LENGTH))

    private fun processUrl(uriString: String): UrlType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val activities = queryActivities(uriString)
                val nonBrowserActivities = keepNonBrowserActivities(activities)

                if (nonBrowserActivities.isNotEmpty()) {
                    nonBrowserActivities.singleOrNull()?.let { resolveInfo ->
                        val nonBrowserIntent = buildNonBrowserIntent(resolveInfo, uriString)
                        return UrlType.AppLink(appIntent = nonBrowserIntent, uriString = uriString)
                    }
                    val excludedComponents = getExcludedComponents(activities)
                    return UrlType.AppLink(excludedComponents = excludedComponents, uriString = uriString)
                }
            } catch (e: URISyntaxException) {
                return UrlType.Unknown(uriString)
            } catch (e: Exception) {
                return UrlType.Web(uriString)
            }
        }
        return UrlType.Web(uriString)
    }

    @SuppressLint("WrongConstant")
    @Throws(URISyntaxException::class)
    private fun queryActivities(uriString: String): MutableList<ResolveInfo> {
        val browsableIntent = Intent.parseUri(uriString, URI_NO_FLAG)
        browsableIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        return context.packageManager.queryIntentActivities(browsableIntent, PackageManager.GET_RESOLVED_FILTER)
    }

    private fun keepNonBrowserActivities(activities: List<ResolveInfo>): List<ResolveInfo> {
        return activities.filter { resolveInfo ->
            resolveInfo.filter != null && !(isBrowserFilter(resolveInfo.filter))
        }
    }

    @SuppressLint("WrongConstant")
    @Throws(URISyntaxException::class)
    private fun buildNonBrowserIntent(
        nonBrowserActivity: ResolveInfo,
        uriString: String
    ): Intent {
        val intent = Intent.parseUri(uriString, URI_NO_FLAG)
        intent.component = ComponentName(nonBrowserActivity.activityInfo.packageName, nonBrowserActivity.activityInfo.name)
        return intent
    }

    private fun getExcludedComponents(activities: List<ResolveInfo>): List<ComponentName> {
        return activities.filter { resolveInfo ->
            resolveInfo.filter != null && isBrowserFilter(resolveInfo.filter)
        }.map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
    }

    private fun isBrowserFilter(filter: IntentFilter) =
        filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0

    private fun checkForIntent(
        scheme: String,
        uriString: String
    ): UrlType {
        val validUriSchemeRegex = Regex("[a-z][a-zA-Z\\d+.-]+")
        if (scheme.matches(validUriSchemeRegex)) {
            return buildIntent(uriString)
        }

        return UrlType.SearchQuery(uriString)
    }

    @SuppressLint("WrongConstant")
    private fun buildIntent(uriString: String): UrlType {
        return try {
            val intent = Intent.parseUri(uriString, URI_NO_FLAG)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val fallbackUrl = intent.getStringExtra(EXTRA_FALLBACK_URL).let { result ->
                if (result.isNullOrEmpty()) {
                    getDeeplinkParam(uriString, "url")
                } else result
            }
            val title = intent.getStringExtra(EXTRA_FALLBACK_URL).let { result ->
                if (result.isNullOrEmpty()) {
                    getDeeplinkParam(uriString, "title")
                } else result
            }
            val fallbackIntent = buildFallbackIntent(fallbackUrl)
            UrlType.NonHttpAppLink(
                uriString = uriString,
                intent = intent,
                fallbackUrl = fallbackUrl,
                title = title,
                fallbackIntent = fallbackIntent
            )
        } catch (e: URISyntaxException) {
            UrlType.Unknown(uriString)
        }
    }

    private fun getDeeplinkParam(uriString: String, param: String): String? {
        val matchingConfig = customDeeplinkSchemes.find { config ->
            uriString.startsWith("${config.scheme}://")
        }

        if (matchingConfig != null) {
            val appLinkData = Uri.parse(uriString)
            val paramName = when (param) {
                "url" -> matchingConfig.urlParam
                "title" -> matchingConfig.titleParam
                else -> param
            }
            return appLinkData.getQueryParameter(paramName)
        }

        return null
    }

    @SuppressLint("WrongConstant")
    private fun buildFallbackIntent(fallbackUrl: String?): Intent? {
        if (fallbackUrl != null && determineType(fallbackUrl) is UrlType.Web) {
            return Intent.parseUri(fallbackUrl, URI_NO_FLAG)
        }
        return null
    }

    override fun determineType(uriString: String?): UrlType {
        if (uriString == null) return UrlType.Web("")

        return determineType(Uri.parse(uriString))
    }

    private fun String.truncate(maxLength: Int): String =
        if (this.length > maxLength) this.substring(0, maxLength) else this

    companion object {
        private const val TEL_SCHEME = "tel"
        private const val TELPROMPT_SCHEME = "telprompt"
        private const val MAILTO_SCHEME = "mailto"
        private const val SMS_SCHEME = "sms"
        private const val SMSTO_SCHEME = "smsto"
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
        private const val ABOUT_SCHEME = "about"
        private const val DATA_SCHEME = "data"
        private const val JAVASCRIPT_SCHEME = "javascript"
        private const val EXTRA_FALLBACK_URL = "browser_fallback_url"
        private const val URI_NO_FLAG = 0
        const val SMS_MAX_LENGTH = 400
        const val PHONE_MAX_LENGTH = 20
        const val EMAIL_MAX_LENGTH = 1000
    }
}
