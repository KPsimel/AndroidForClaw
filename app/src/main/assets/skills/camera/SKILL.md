---
name: camera
description: 使用设备摄像头拍照或录像。支持列出摄像头、拍照（snap）和录短视频（clip）。
always: false
---

# Camera Skill

通过设备摄像头拍照或录制短视频。

## 使用场景

- 用户要求"拍个照片看看周围有什么"
- 用户要求"用前置摄像头拍一张"
- 用户要求"录一段视频"
- 用户要求"列出可用的摄像头"

## 工具调用

使用 `camera` 工具，参数：

### list — 列出摄像头
```json
{"action": "list"}
```

### snap — 拍照
```json
{
  "action": "snap",
  "facing": "back",
  "quality": 0.95,
  "max_width": 1600
}
```
- `facing`: "front"（前置）或 "back"（后置），默认 "back"
- `quality`: JPEG 质量 0.1-1.0，默认 0.95
- `max_width`: 最大图片宽度（像素），默认 1600
- `device_id`: 指定摄像头 ID（可选，从 list 获取）

### clip — 录像
```json
{
  "action": "clip",
  "facing": "back",
  "duration_ms": 3000,
  "include_audio": true
}
```
- `facing`: "front" 或 "back"，默认 "back"
- `duration_ms`: 录像时长（毫秒），默认 3000，最大 60000
- `include_audio`: 是否录制音频，默认 true
- `device_id`: 指定摄像头 ID（可选）

## 注意事项

- 需要 CAMERA 权限（拍照/录像）和 RECORD_AUDIO 权限（录像含音频）
- 如果权限未授权，会返回错误提示用户去系统设置中授权
- 拍照返回 JPEG 格式（base64），自动处理 EXIF 旋转和尺寸压缩（< 5MB）
- 录像返回 MP4 格式（base64），文件上限 18MB
- 照片/视频会保存到工作空间 camera/ 目录
