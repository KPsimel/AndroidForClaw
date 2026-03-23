/**
 * OpenClaw Source Reference:
 * - @tencent-weixin/openclaw-weixin/src/api/types.ts
 *
 * Weixin protocol types. API uses JSON over HTTP.
 */
package com.xiaomo.weixin.api

import com.google.gson.annotations.SerializedName

// ── Media Types ─────────────────────────────────────────────────────────────

object UploadMediaType {
    const val IMAGE = 1
    const val VIDEO = 2
    const val FILE = 3
    const val VOICE = 4
}

object MessageItemType {
    const val NONE = 0
    const val TEXT = 1
    const val IMAGE = 2
    const val VOICE = 3
    const val FILE = 4
    const val VIDEO = 5
}

object MessageType {
    const val NONE = 0
    const val USER = 1
    const val BOT = 2
}

object MessageState {
    const val NEW = 0
    const val GENERATING = 1
    const val FINISH = 2
}

object TypingStatus {
    const val TYPING = 1
    const val CANCEL = 2
}

// ── CDN & Media ─────────────────────────────────────────────────────────────

data class CDNMedia(
    @SerializedName("encrypt_query_param") val encryptQueryParam: String? = null,
    @SerializedName("aes_key") val aesKey: String? = null,
    @SerializedName("encrypt_type") val encryptType: Int? = null,
)

data class TextItem(
    val text: String? = null,
)

data class ImageItem(
    val media: CDNMedia? = null,
    @SerializedName("thumb_media") val thumbMedia: CDNMedia? = null,
    val aeskey: String? = null,
    val url: String? = null,
    @SerializedName("mid_size") val midSize: Int? = null,
    @SerializedName("thumb_size") val thumbSize: Int? = null,
    @SerializedName("thumb_height") val thumbHeight: Int? = null,
    @SerializedName("thumb_width") val thumbWidth: Int? = null,
    @SerializedName("hd_size") val hdSize: Int? = null,
)

data class VoiceItem(
    val media: CDNMedia? = null,
    @SerializedName("encode_type") val encodeType: Int? = null,
    @SerializedName("bits_per_sample") val bitsPerSample: Int? = null,
    @SerializedName("sample_rate") val sampleRate: Int? = null,
    val playtime: Int? = null,
    val text: String? = null,
)

data class FileItem(
    val media: CDNMedia? = null,
    @SerializedName("file_name") val fileName: String? = null,
    val md5: String? = null,
    val len: String? = null,
)

data class VideoItem(
    val media: CDNMedia? = null,
    @SerializedName("video_size") val videoSize: Int? = null,
    @SerializedName("play_length") val playLength: Int? = null,
    @SerializedName("video_md5") val videoMd5: String? = null,
    @SerializedName("thumb_media") val thumbMedia: CDNMedia? = null,
    @SerializedName("thumb_size") val thumbSize: Int? = null,
    @SerializedName("thumb_height") val thumbHeight: Int? = null,
    @SerializedName("thumb_width") val thumbWidth: Int? = null,
)

data class RefMessage(
    @SerializedName("message_item") val messageItem: MessageItem? = null,
    val title: String? = null,
)

// ── Message ─────────────────────────────────────────────────────────────────

data class MessageItem(
    val type: Int? = null,
    @SerializedName("create_time_ms") val createTimeMs: Long? = null,
    @SerializedName("update_time_ms") val updateTimeMs: Long? = null,
    @SerializedName("is_completed") val isCompleted: Boolean? = null,
    @SerializedName("msg_id") val msgId: String? = null,
    @SerializedName("ref_msg") val refMsg: RefMessage? = null,
    @SerializedName("text_item") val textItem: TextItem? = null,
    @SerializedName("image_item") val imageItem: ImageItem? = null,
    @SerializedName("voice_item") val voiceItem: VoiceItem? = null,
    @SerializedName("file_item") val fileItem: FileItem? = null,
    @SerializedName("video_item") val videoItem: VideoItem? = null,
)

data class WeixinMessage(
    val seq: Int? = null,
    @SerializedName("message_id") val messageId: Long? = null,
    @SerializedName("from_user_id") val fromUserId: String? = null,
    @SerializedName("to_user_id") val toUserId: String? = null,
    @SerializedName("client_id") val clientId: String? = null,
    @SerializedName("create_time_ms") val createTimeMs: Long? = null,
    @SerializedName("update_time_ms") val updateTimeMs: Long? = null,
    @SerializedName("delete_time_ms") val deleteTimeMs: Long? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("group_id") val groupId: String? = null,
    @SerializedName("message_type") val messageType: Int? = null,
    @SerializedName("message_state") val messageState: Int? = null,
    @SerializedName("item_list") val itemList: List<MessageItem>? = null,
    @SerializedName("context_token") val contextToken: String? = null,
)

// ── Request / Response ──────────────────────────────────────────────────────

data class BaseInfo(
    @SerializedName("channel_version") val channelVersion: String? = null,
)

data class GetUpdatesRequest(
    @SerializedName("get_updates_buf") val getUpdatesBuf: String = "",
    @SerializedName("base_info") val baseInfo: BaseInfo? = null,
)

data class GetUpdatesResponse(
    val ret: Int? = null,
    val errcode: Int? = null,
    val errmsg: String? = null,
    val msgs: List<WeixinMessage>? = null,
    @SerializedName("get_updates_buf") val getUpdatesBuf: String? = null,
    @SerializedName("longpolling_timeout_ms") val longpollingTimeoutMs: Long? = null,
)

data class SendMessageRequest(
    val msg: WeixinMessage? = null,
    @SerializedName("base_info") val baseInfo: BaseInfo? = null,
)

data class SendTypingRequest(
    @SerializedName("ilink_user_id") val ilinkUserId: String? = null,
    @SerializedName("typing_ticket") val typingTicket: String? = null,
    val status: Int? = null,
    @SerializedName("base_info") val baseInfo: BaseInfo? = null,
)

data class GetConfigRequest(
    @SerializedName("ilink_user_id") val ilinkUserId: String? = null,
    @SerializedName("context_token") val contextToken: String? = null,
    @SerializedName("base_info") val baseInfo: BaseInfo? = null,
)

data class GetConfigResponse(
    val ret: Int? = null,
    val errmsg: String? = null,
    @SerializedName("typing_ticket") val typingTicket: String? = null,
)

data class GetUploadUrlRequest(
    val filekey: String? = null,
    @SerializedName("media_type") val mediaType: Int? = null,
    @SerializedName("to_user_id") val toUserId: String? = null,
    val rawsize: Long? = null,
    val rawfilemd5: String? = null,
    val filesize: Long? = null,
    @SerializedName("thumb_rawsize") val thumbRawsize: Long? = null,
    @SerializedName("thumb_rawfilemd5") val thumbRawfilemd5: String? = null,
    @SerializedName("thumb_filesize") val thumbFilesize: Long? = null,
    @SerializedName("no_need_thumb") val noNeedThumb: Boolean? = null,
    val aeskey: String? = null,
    @SerializedName("base_info") val baseInfo: BaseInfo? = null,
)

data class GetUploadUrlResponse(
    @SerializedName("upload_param") val uploadParam: String? = null,
    @SerializedName("thumb_upload_param") val thumbUploadParam: String? = null,
)

// ── QR Login ────────────────────────────────────────────────────────────────

data class QRCodeResponse(
    val qrcode: String? = null,
    @SerializedName("qrcode_img_content") val qrcodeImgContent: String? = null,
)

data class QRStatusResponse(
    val status: String? = null, // "wait", "scaned", "confirmed", "expired"
    @SerializedName("bot_token") val botToken: String? = null,
    @SerializedName("ilink_bot_id") val ilinkBotId: String? = null,
    val baseurl: String? = null,
    @SerializedName("ilink_user_id") val ilinkUserId: String? = null,
)
