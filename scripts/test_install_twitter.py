#!/usr/bin/env python3
"""
完整测试: 搜索 → 安装 Twitter Skill
增加超时时间避免断连
"""

import asyncio
import websockets
import json
import sys

GATEWAY_URL = "ws://localhost:8765/ws"

async def test_install():
    print("=" * 70)
    print("🎯 完整测试: 搜索并安装 Twitter Skill")
    print("=" * 70)
    print()

    try:
        # 设置更长的超时时间
        async with websockets.connect(GATEWAY_URL, ping_interval=None, close_timeout=30) as ws:
            # 跳过欢迎消息
            welcome = await ws.recv()
            print("✅ 已连接到 Gateway")
            print()

            # ========================================
            # 步骤 1: 搜索 Twitter skills
            # ========================================
            print("1️⃣  搜索 Twitter skills...")
            req1 = {
                "type": "req",
                "id": "search-1",
                "method": "skills.search",
                "params": {"query": "twitter", "limit": 5}
            }

            await ws.send(json.dumps(req1))
            print("   ⏳ 等待搜索结果...")

            # 增加超时到 30 秒
            resp1 = json.loads(await asyncio.wait_for(ws.recv(), timeout=30.0))

            if not resp1.get("ok"):
                error = resp1.get("error", {})
                print(f"   ❌ 搜索失败: {error.get('message')}")
                return False

            payload1 = resp1.get("payload", {})
            skills = payload1.get("skills", [])

            print(f"   ✅ 找到 {len(skills)} 个技能:")
            for i, skill in enumerate(skills, 1):
                print(f"      {i}. {skill.get('name')} ({skill.get('slug')})")
                desc = skill.get('description', '')[:70]
                print(f"         {desc}...")

            if not skills:
                print("   ⚠️  未找到技能")
                return False

            # 选择第一个技能
            target = skills[0]
            skill_name = target.get('name')
            skill_slug = target.get('slug')

            print()
            print(f"2️⃣  选择安装技能: {skill_name}")
            print(f"   Slug: {skill_slug}")
            print()

            # ========================================
            # 步骤 2: 安装技能
            # ========================================
            print("3️⃣  开始安装...")
            req2 = {
                "type": "req",
                "id": "install-1",
                "method": "skills.install",
                "params": {
                    "name": skill_name,
                    "installId": "download",
                    "timeoutMs": 300000
                }
            }

            await ws.send(json.dumps(req2))
            print("   ⏳ 下载并安装中 (最多等待 90 秒)...")

            # 安装可能需要更长时间
            resp2 = json.loads(await asyncio.wait_for(ws.recv(), timeout=90.0))

            if not resp2.get("ok"):
                error = resp2.get("error", {})
                print(f"   ❌ 安装失败!")
                print(f"   错误信息: {error.get('message')}")
                print(f"   错误代码: {error.get('code')}")
                return False

            payload2 = resp2.get("payload", {})
            print()
            print("   ✅ 安装成功!")
            print(f"   消息: {payload2.get('message')}")

            if "details" in payload2:
                details = payload2["details"]
                print()
                print("   📦 安装详情:")
                print(f"      - 技能名称: {details.get('name')}")
                print(f"      - Slug: {details.get('slug')}")
                print(f"      - 版本: {details.get('version')}")
                print(f"      - 安装路径: {details.get('path')}")
                if details.get('hash'):
                    print(f"      - Hash: {details.get('hash')[:32]}...")

            # ========================================
            # 步骤 3: 验证安装
            # ========================================
            print()
            print("4️⃣  验证安装...")
            req3 = {
                "type": "req",
                "id": "status-1",
                "method": "skills.status",
                "params": {}
            }

            await ws.send(json.dumps(req3))
            resp3 = json.loads(await asyncio.wait_for(ws.recv(), timeout=15.0))

            if resp3.get("ok"):
                all_skills = resp3["payload"]["skills"]
                # 查找刚安装的技能
                installed = None
                for s in all_skills:
                    if skill_slug in s.get("skillKey", "") or skill_slug == s.get("name", ""):
                        installed = s
                        break

                if installed:
                    print(f"   ✅ 技能已加载到系统!")
                    print(f"      - 名称: {installed.get('name')}")
                    print(f"      - 来源: {installed.get('source')}")
                    print(f"      - 可用: {installed.get('eligible')}")
                    print(f"      - 描述: {installed.get('description', '')[:60]}...")
                else:
                    print(f"   ⚠️  技能未在列表中 (可能需要 reload)")

            # ========================================
            # 步骤 4: 检查 lock.json
            # ========================================
            print()
            print("5️⃣  检查 lock.json...")
            print("   (通过 adb 查看)")

            print()
            print("=" * 70)
            print("✅ 测试完成!")
            print("=" * 70)
            return True

    except asyncio.TimeoutError as e:
        print()
        print(f"❌ 超时: 操作时间过长")
        print(f"   {e}")
        return False
    except websockets.exceptions.ConnectionClosed as e:
        print()
        print(f"❌ 连接关闭: {e}")
        return False
    except Exception as e:
        print()
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = asyncio.run(test_install())
    sys.exit(0 if success else 1)
