package it.polimi.webeep.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of core_webservice_get_site_info. */
@Serializable
data class SiteInfoDto(
    @SerialName("sitename") val sitename: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("firstname") val firstname: String = "",
    @SerialName("lastname") val lastname: String = "",
    @SerialName("fullname") val fullname: String = "",
    @SerialName("lang") val lang: String = "",
    @SerialName("userid") val userid: Int = 0,
    @SerialName("siteurl") val siteurl: String = "",
    @SerialName("userpictureurl") val userpictureurl: String = "",
    @SerialName("downloadfiles") val downloadfiles: Int = 0,
    @SerialName("release") val release: String? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("functions") val functions: List<FunctionDto>? = null
)

@Serializable
data class FunctionDto(
    @SerialName("name") val name: String = "",
    @SerialName("version") val version: String? = null
)
